#include "resources/image.hpp"

#include <string>

#include "device/vulkan_error.hpp"
#include "resources/memory_type.hpp"

namespace minemod::renderer {

namespace {

void require_format_support(VkPhysicalDevice device, const ImageDescription& description) {
    VkImageFormatProperties supported{};
    const VkResult support = vkGetPhysicalDeviceImageFormatProperties(
        device, description.format, VK_IMAGE_TYPE_2D, VK_IMAGE_TILING_OPTIMAL, description.usage, 0, &supported);
    if (support == VK_ERROR_FORMAT_NOT_SUPPORTED) {
        throw UnsupportedSystem(
            "the device cannot create images of VkFormat " + std::to_string(description.format) + " with usage "
            + std::to_string(description.usage));
    }
    require_success(support, "querying image format support");
}

}

Image::Image(const Device& device, const ImageDescription& description) : description_(description) {
    require_format_support(device.physical(), description);

    const VkImageCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_IMAGE_CREATE_INFO,
        .imageType = VK_IMAGE_TYPE_2D,
        .format = description.format,
        .extent = {description.extent.width, description.extent.height, 1},
        .mipLevels = 1,
        .arrayLayers = 1,
        .samples = VK_SAMPLE_COUNT_1_BIT,
        .tiling = VK_IMAGE_TILING_OPTIMAL,
        .usage = description.usage,
        .sharingMode = VK_SHARING_MODE_EXCLUSIVE,
        .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
    };
    image_ = create_device_handle<VkImage>(device.get(), create_info, vkCreateImage, vkDestroyImage, "creating an image");

    VkMemoryRequirements requirements{};
    vkGetImageMemoryRequirements(device.get(), image_.get(), &requirements);
    const VkMemoryAllocateInfo allocation{
        .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
        .allocationSize = requirements.size,
        .memoryTypeIndex = find_memory_type(
            device.physical(), requirements.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
    };
    memory_ = create_device_handle<VkDeviceMemory>(
        device.get(), allocation, vkAllocateMemory, vkFreeMemory, "allocating image memory");
    require_success(vkBindImageMemory(device.get(), image_.get(), memory_.get(), 0), "binding image memory");

    const VkImageViewCreateInfo view_info{
        .sType = VK_STRUCTURE_TYPE_IMAGE_VIEW_CREATE_INFO,
        .image = image_.get(),
        .viewType = VK_IMAGE_VIEW_TYPE_2D,
        .format = description.format,
        .subresourceRange = {description.aspect, 0, 1, 0, 1},
    };
    view_ = create_device_handle<VkImageView>(
        device.get(), view_info, vkCreateImageView, vkDestroyImageView, "creating an image view");
}

}
