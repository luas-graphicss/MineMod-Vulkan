#include "memory/shared_image.hpp"

#include <string>

#include "device/vulkan_error.hpp"
#include "memory/external_support.hpp"
#include "textures/mip_chain.hpp"

namespace minemod::interop {
namespace {

std::string describe(const SharedImageDescription& description) {
    return std::to_string(description.extent.width) + "x" + std::to_string(description.extent.height)
        + " images of VkFormat " + std::to_string(description.format) + " with usage "
        + std::to_string(description.usage) + " and " + std::to_string(description.mip_levels) + " mip level(s)";
}

bool fits(const SharedImageDescription& description, const ExternalSupport& support) {
    return description.extent.width <= support.max_extent.width
        && description.extent.height <= support.max_extent.height
        && description.mip_levels >= 1
        && description.mip_levels <= support.max_mip_levels
        && description.mip_levels <= renderer::full_mip_level_count(description.extent);
}

void require_exportable(VkPhysicalDevice device, const SharedImageDescription& description) {
    const ExternalSupport support = query_external_support(device, description.format, description.usage);
    if (!support.exportable || !fits(description, support)) {
        throw renderer::UnsupportedSystem(
            "the device cannot export " + describe(description) + " to the OpenGL provider");
    }
}

}

SharedImage::SharedImage(const renderer::Device& device, const SharedImageDescription& description)
    : memory_export_(device.get()) {
    require_exportable(device.physical(), description);

    const VkExternalMemoryImageCreateInfo external_image{
        .sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_IMAGE_CREATE_INFO,
        .handleTypes = shared_handle_type,
    };
    const VkImageCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
        .pNext = &external_image,
        .imageType = VK_IMAGE_TYPE_2D,
        .format = description.format,
        .extent = {description.extent.width, description.extent.height, 1},
        .mipLevels = description.mip_levels,
        .arrayLayers = 1,
        .samples = VK_SAMPLE_COUNT_1_BIT,
        .tiling = VK_IMAGE_TILING_OPTIMAL,
        .usage = description.usage,
        .sharingMode = VK_SHARING_MODE_EXCLUSIVE,
        .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
    };
    image_ = renderer::create_device_handle<VkImage>(
        device.get(), create_info, vkCreateImage, vkDestroyImage, "creating a shared image");

    VkMemoryRequirements requirements{};
    vkGetImageMemoryRequirements(device.get(), image_.get(), &requirements);
    allocation_size_ = requirements.size;
    memory_ = allocate_exportable_memory(
        device, requirements, {.sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO, .image = image_.get()});
    renderer::require_success(
        vkBindImageMemory(device.get(), image_.get(), memory_.get(), 0), "binding shared image memory");
}

ExportedHandle SharedImage::export_memory() const {
    return memory_export_.export_handle(memory_.get());
}

}
