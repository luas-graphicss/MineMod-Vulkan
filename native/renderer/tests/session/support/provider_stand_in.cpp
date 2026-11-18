#include "session/support/provider_stand_in.hpp"

#include <cstddef>
#include <cstdint>
#include <span>
#include <utility>
#include <vector>

#include "device/vulkan_error.hpp"
#include "frame/frame_readback.hpp"
#include "memory/external_support.hpp"
#include "resources/image_layout.hpp"
#include "support/memory_import.hpp"

namespace minemod::scene::test_support {

namespace {

constexpr VkImageUsageFlags shared_color_usage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT
    | VK_IMAGE_USAGE_TRANSFER_SRC_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;
constexpr VkImageSubresourceRange whole_color{VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1};
constexpr VkAccessFlags2 any_access = VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT;

constexpr renderer::LayoutChange transfer_source_to_destination{
    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_NONE,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_TRANSFER_WRITE_BIT,
};

renderer::LayoutChange handed_over_to_transfer_source(VkImageLayout handoff_layout) {
    return renderer::LayoutChange{
        handoff_layout,
        VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
        VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,
        any_access,
        VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
        VK_ACCESS_2_TRANSFER_READ_BIT,
    };
}

renderer::LayoutChange transfer_destination_to_handed_over(VkImageLayout handoff_layout) {
    return renderer::LayoutChange{
        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
        handoff_layout,
        VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
        VK_ACCESS_2_TRANSFER_WRITE_BIT,
        VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,
        any_access,
    };
}

renderer::DeviceHandle<VkImage> create_image(const renderer::Device& device, const minemod_shared_attachment& color) {
    const VkExternalMemoryImageCreateInfo external_image{
        .sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO,
        .handleTypes = static_cast<VkExternalMemoryHandleTypeFlags>(interop::shared_handle_type),
    };
    const VkImageCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
        .pNext = &external_image,
        .imageType = VK_IMAGE_TYPE_2D,
        .format = static_cast<VkFormat>(color.vulkan_format),
        .extent = {color.width, color.height, 1},
        .mipLevels = 1,
        .arrayLayers = 1,
        .samples = VK_SAMPLE_COUNT_1_BIT,
        .tiling = VK_IMAGE_TILING_OPTIMAL,
        .usage = shared_color_usage,
        .sharingMode = VK_SHARING_MODE_EXCLUSIVE,
        .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
    };
    return renderer::create_device_handle<VkImage>(
        device.get(), create_info, vkCreateImage, vkDestroyImage, "creating the image a provider imports");
}

std::vector<std::uint8_t> bytes_of(std::span<const std::byte> rgba) {
    std::vector<std::uint8_t> bytes(rgba.size());
    for (std::size_t index = 0; index < rgba.size(); ++index) {
        bytes[index] = std::to_integer<std::uint8_t>(rgba[index]);
    }
    return bytes;
}

}

ProviderStandIn::ProviderStandIn(const minemod_shared_attachment& color, interop::ExportedHandle memory)
    : extent_{color.width, color.height},
      handoff_layout_(static_cast<VkImageLayout>(color.vulkan_layout)),
      image_(create_image(gpu_.device(), color)),
      acquire_(interop::OwnershipTransfer::acquire_for(gpu_.device().queue_family(), imported_color())),
      release_(interop::OwnershipTransfer::release_from(gpu_.device().queue_family(), imported_color())) {
    memory_ = interop::test_support::import_image_memory(
        gpu_.device(), image_.get(), color.allocation_bytes, std::move(memory));
    renderer::require_success(
        vkBindImageMemory(gpu_.device().get(), image_.get(), memory_.get(), 0),
        "binding the memory of a shared attachment");
}

DrawnFrame ProviderStandIn::fill_after_reading(const std::array<float, 4>& fill) {
    const renderer::Device& device = gpu_.device();
    device.submit([this](VkCommandBuffer commands) {
        acquire_.record(commands);
        renderer::change_layout(
            commands, image_.get(), VK_IMAGE_ASPECT_COLOR_BIT, handed_over_to_transfer_source(handoff_layout_));
    });
    const renderer::FramePixels seen = renderer::read_back_color(device, image_.get(), extent_);
    device.submit([this, &fill](VkCommandBuffer commands) {
        const VkClearColorValue color{.float32 = {fill[0], fill[1], fill[2], fill[3]}};
        renderer::change_layout(commands, image_.get(), VK_IMAGE_ASPECT_COLOR_BIT, transfer_source_to_destination);
        vkCmdClearColorImage(commands, image_.get(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, &color, 1, &whole_color);
        renderer::change_layout(
            commands, image_.get(), VK_IMAGE_ASPECT_COLOR_BIT, transfer_destination_to_handed_over(handoff_layout_));
        release_.record(commands);
    });
    return DrawnFrame(extent_.width, bytes_of(seen.rgba()));
}

interop::HandoffResources ProviderStandIn::imported_color() const {
    interop::HandoffResources resources;
    resources.images.push_back({image_.get(), VK_IMAGE_ASPECT_COLOR_BIT, handoff_layout_});
    return resources;
}

}
