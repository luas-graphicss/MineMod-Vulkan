#include "memory/shared_image.hpp"

#include <string>

#include "device/vulkan_error.hpp"
#include "memory/external_support.hpp"
#include "resources/memory_type.hpp"

namespace minemod::interop {
namespace {

std::string describe(const SharedImageDescription& description) {
    return std::to_string(description.extent.width) + "x" + std::to_string(description.extent.height)
        + " images of VkFormat " + std::to_string(description.format) + " with usage "
        + std::to_string(description.usage);
}

void require_exportable(VkPhysicalDevice device, const SharedImageDescription& description) {
    const ExternalSupport support = query_external_support(device, description.format, description.usage);
    const bool fits = description.extent.width <= support.max_extent.width
        && description.extent.height <= support.max_extent.height;
    if (!support.exportable || !fits) {
        throw renderer::UnsupportedSystem(
            "the device cannot export " + describe(description) + " to the OpenGL provider");
    }
}

PFN_vkGetMemoryFdKHR find_memory_export(VkDevice device) {
    const auto export_memory =
        reinterpret_cast<PFN_vkGetMemoryFdKHR>(vkGetDeviceProcAddr(device, "vkGetMemoryFdKHR"));
    if (export_memory == nullptr) {
        throw renderer::UnsupportedSystem(
            "the device was created without " VK_KHR_EXTERNAL_MEMORY_FD_EXTENSION_NAME ", so it cannot share images");
    }
    return export_memory;
}

}

SharedImage::SharedImage(const renderer::Device& device, const SharedImageDescription& description)
    : device_(device.get()), get_memory_descriptor_(find_memory_export(device.get())) {
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
        .mipLevels = 1,
        .arrayLayers = 1,
        .samples = VK_SAMPLE_COUNT_1_BIT,
        .tiling = VK_IMAGE_TILING_OPTIMAL,
        .usage = description.usage,
        .sharingMode = VK_SHARING_MODE_EXCLUSIVE,
        .initialLayout = VK_IMAGE_LAYOUT_UNDEFINED,
    };
    image_ = renderer::create_device_handle<VkImage>(
        device_, create_info, vkCreateImage, vkDestroyImage, "creating a shared image");

    VkMemoryRequirements requirements{};
    vkGetImageMemoryRequirements(device_, image_.get(), &requirements);
    allocation_size_ = requirements.size;

    const VkMemoryDedicatedAllocateInfo dedicated{
        .sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO,
        .image = image_.get(),
    };
    const VkExportMemoryAllocateInfo exported{
        .sType = VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO,
        .pNext = &dedicated,
        .handleTypes = shared_handle_type,
    };
    const VkMemoryAllocateInfo allocation{
        .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
        .pNext = &exported,
        .allocationSize = requirements.size,
        .memoryTypeIndex = renderer::find_memory_type(
            device.physical(), requirements.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
    };
    memory_ = renderer::create_device_handle<VkDeviceMemory>(
        device_, allocation, vkAllocateMemory, vkFreeMemory, "allocating shared image memory");
    renderer::require_success(
        vkBindImageMemory(device_, image_.get(), memory_.get(), 0), "binding shared image memory");
}

OwnedDescriptor SharedImage::export_memory() const {
    const VkMemoryGetFdInfoKHR request{
        .sType = VK_STRUCTURE_TYPE_MEMORY_GET_FD_INFO_KHR,
        .memory = memory_.get(),
        .handleType = shared_handle_type,
    };
    int descriptor = -1;
    renderer::require_success(get_memory_descriptor_(device_, &request, &descriptor), "exporting shared image memory");
    return OwnedDescriptor(descriptor);
}

}
