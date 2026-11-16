#include "sharing/interop_shared_targets.hpp"

#include "frame/frame_targets.hpp"

namespace minemod::scene {

namespace {

constexpr VkImageUsageFlags provider_usage =
    VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;

constexpr AttachmentRole color_role{
    renderer::frame_color_format,
    VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | provider_usage,
    VK_IMAGE_ASPECT_COLOR_BIT,
    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
};

constexpr AttachmentRole depth_stencil_role{
    VK_FORMAT_D32_SFLOAT_S8_UINT,
    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT | provider_usage,
    VK_IMAGE_ASPECT_DEPTH_BIT | VK_IMAGE_ASPECT_STENCIL_BIT,
    VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
};

renderer::DeviceHandle<VkImageView> create_view(
    const renderer::Device& device, VkImage image, const AttachmentRole& role) {
    const VkImageViewCreateInfo view_info{
        .sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
        .image = image,
        .viewType = VK_IMAGE_VIEW_TYPE_2D,
        .format = role.format,
        .subresourceRange = {role.aspect, 0, 1, 0, 1},
    };
    return renderer::create_device_handle<VkImageView>(
        device.get(), view_info, vkCreateImageView, vkDestroyImageView, "creating a shared attachment view");
}

}

InteropSharedTargets::Attachment::Attachment(
    const renderer::Device& device, VkExtent2D extent, const AttachmentRole& role)
    : image(device, {extent, role.format, role.usage}),
      view(create_view(device, image.get(), role)),
      described{image.get(), view.get(), role.format, role.aspect, role.handoff_layout, image.allocation_size()} {}

InteropSharedTargets::InteropSharedTargets(const renderer::Device& device, VkExtent2D extent)
    : attachments_{Attachment(device, extent, color_role), Attachment(device, extent, depth_stencil_role)} {}

const AttachmentImage& InteropSharedTargets::attachment(SharedAttachment which) const noexcept {
    return owned(which).described;
}

std::int64_t InteropSharedTargets::export_memory(SharedAttachment which) const {
    return owned(which).image.export_memory().release();
}

const InteropSharedTargets::Attachment& InteropSharedTargets::owned(SharedAttachment which) const noexcept {
    return attachments_[static_cast<std::size_t>(which)];
}

}
