#include "memory/external_support.hpp"

#include "device/vulkan_error.hpp"

namespace minemod::interop {

ExternalSupport query_external_support(VkPhysicalDevice device, VkFormat format, VkImageUsageFlags usage) {
    const VkPhysicalDeviceExternalImageFormatInfo external_request{
        .sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_IMAGE_FORMAT_INFO,
        .handleType = shared_handle_type,
    };
    const VkPhysicalDeviceImageFormatInfo2 request{
        .sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_IMAGE_FORMAT_INFO_2,
        .pNext = &external_request,
        .format = format,
        .type = VK_IMAGE_TYPE_2D,
        .tiling = VK_IMAGE_TILING_OPTIMAL,
        .usage = usage,
    };
    VkExternalImageFormatProperties external_answer{.sType = VK_STRUCTURE_TYPE_EXTERNAL_IMAGE_FORMAT_PROPERTIES};
    VkImageFormatProperties2 answer{
        .sType = VK_STRUCTURE_TYPE_IMAGE_FORMAT_PROPERTIES_2,
        .pNext = &external_answer,
    };

    const VkResult support = vkGetPhysicalDeviceImageFormatProperties2(device, &request, &answer);
    if (support == VK_ERROR_FORMAT_NOT_SUPPORTED) {
        return ExternalSupport{};
    }
    renderer::require_success(support, "querying external image format support");

    const VkExternalMemoryFeatureFlags features = external_answer.externalMemoryProperties.externalMemoryFeatures;
    const VkExtent3D& max_extent = answer.imageFormatProperties.maxExtent;
    return ExternalSupport{
        (features & VK_EXTERNAL_MEMORY_FEATURE_EXPORTABLE_BIT) != 0,
        {max_extent.width, max_extent.height},
        answer.imageFormatProperties.maxMipLevels,
    };
}

}
