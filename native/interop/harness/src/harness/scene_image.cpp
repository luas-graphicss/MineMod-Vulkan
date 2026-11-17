#include "harness/scene_image.hpp"

#include <cstddef>
#include <stdexcept>

#include "resources/buffer.hpp"

namespace minemod::interop::harness {
namespace {

constexpr std::size_t bytes_per_texel = 4;
constexpr VkAccessFlags2 any_access = VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT;
constexpr VkImageUsageFlags transfers_and_sampling =
    VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;

struct AttachmentRole {
    VkImageUsageFlags usage;
    VkImageAspectFlags aspect;
    VkImageLayout layout;
};

AttachmentRole role_of(SceneImageKind kind) noexcept {
    if (kind == SceneImageKind::color) {
        return {
            VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
            VK_IMAGE_ASPECT_COLOR_BIT,
            VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
        };
    }
    return {
        VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
        VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT,
        VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
    };
}

void move_layout(
    VkCommandBuffer commands, VkImage image, VkImageAspectFlags aspect, VkImageLayout from, VkImageLayout to) {
    const VkImageMemoryBarrier2 barrier{
        .sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2,
        .srcStageMask = VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,
        .srcAccessMask = any_access,
        .dstStageMask = VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,
        .dstAccessMask = any_access,
        .oldLayout = from,
        .newLayout = to,
        .srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
        .dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
        .image = image,
        .subresourceRange = {aspect, 0, VK_REMAINING_MIP_LEVELS, 0, VK_REMAINING_ARRAY_LAYERS},
    };
    const VkDependencyInfo dependency{
        .sType = VK_STRUCTURE_TYPE_DEPENDENCY_INFO,
        .imageMemoryBarrierCount = 1,
        .pImageMemoryBarriers = &barrier,
    };
    vkCmdPipelineBarrier2(commands, &dependency);
}

}

VkImageUsageFlags scene_image_usage(SceneImageKind kind) noexcept {
    return role_of(kind).usage | transfers_and_sampling;
}

SceneImage::SceneImage(const renderer::Device& device, const SceneImageDescription& description)
    : device_(device),
      description_(description),
      image_(device, {description.extent, description.format, scene_image_usage(description.kind)}) {
    const AttachmentRole role = role_of(description.kind);
    const VkImageViewCreateInfo view_info{
        .sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
        .image = image_.get(),
        .viewType = VK_IMAGE_VIEW_TYPE_2D,
        .format = description.format,
        .subresourceRange = {role.aspect, 0, 1, 0, 1},
    };
    view_ = renderer::create_device_handle<VkImageView>(
        device.get(), view_info, vkCreateImageView, vkDestroyImageView, "creating a scene image view");
    device.submit([&](VkCommandBuffer commands) {
        move_layout(commands, image_.get(), role.aspect, VK_IMAGE_LAYOUT_UNDEFINED, role.layout);
    });
}

SharedImageUse SceneImage::handoff_use() const noexcept {
    const AttachmentRole role = role_of(description_.kind);
    return {image_.get(), role.aspect, role.layout};
}

void SceneImage::record_clear(VkCommandBuffer commands, const VkRect2D& area, const VkClearValue& value) const {
    const AttachmentRole role = role_of(description_.kind);
    const bool is_color = description_.kind == SceneImageKind::color;
    const VkRenderingAttachmentInfo attachment{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = view_.get(),
        .imageLayout = role.layout,
        .loadOp = VK_ATTACHMENT_LOAD_OP_LOAD,
        .storeOp = VK_ATTACHMENT_STORE_OP_STORE,
    };
    const VkRenderingInfo rendering{
        .sType = VK_STRUCTURE_TYPE_RENDERING_INFO,
        .renderArea = {{0, 0}, description_.extent},
        .layerCount = 1,
        .colorAttachmentCount = is_color ? 1u : 0u,
        .pColorAttachments = is_color ? &attachment : nullptr,
        .pDepthAttachment = is_color ? nullptr : &attachment,
        .pStencilAttachment = is_color ? nullptr : &attachment,
    };
    const VkClearAttachment cleared{.aspectMask = role.aspect, .colorAttachment = 0, .clearValue = value};
    const VkClearRect rectangle{.rect = area, .baseArrayLayer = 0, .layerCount = 1};

    vkCmdBeginRendering(commands, &rendering);
    vkCmdClearAttachments(commands, 1, &cleared, 1, &rectangle);
    vkCmdEndRendering(commands);
}

void SceneImage::clear(const VkRect2D& area, const VkClearValue& value) const {
    device_.submit([&](VkCommandBuffer commands) { record_clear(commands, area, value); });
}

renderer::FramePixels SceneImage::read_color() const {
    if (description_.kind != SceneImageKind::color) {
        throw std::logic_error("only a color scene image is read back as texels");
    }

    const VkExtent2D extent = description_.extent;
    const VkDeviceSize byte_count = VkDeviceSize{extent.width} * extent.height * bytes_per_texel;
    const renderer::Buffer staging(device_, byte_count, VK_BUFFER_USAGE_TRANSFER_DST_BIT, renderer::host_memory);
    const AttachmentRole role = role_of(description_.kind);

    device_.submit([&](VkCommandBuffer commands) {
        const VkBufferImageCopy region{
            .imageSubresource = {role.aspect, 0, 0, 1},
            .imageExtent = {extent.width, extent.height, 1},
        };
        move_layout(commands, image_.get(), role.aspect, role.layout, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL);
        vkCmdCopyImageToBuffer(
            commands, image_.get(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, staging.get(), 1, &region);
        move_layout(commands, image_.get(), role.aspect, VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, role.layout);
    });
    return renderer::FramePixels(extent, staging.read());
}

}
