#include "support/memory_import.hpp"

#include "memory/external_support.hpp"
#include "resources/memory_type.hpp"

namespace minemod::interop::test_support {

renderer::DeviceHandle<VkDeviceMemory> import_image_memory(
    const renderer::Device& device, VkImage image, VkDeviceSize allocation_bytes, ExportedHandle memory) {
    VkMemoryRequirements requirements{};
    vkGetImageMemoryRequirements(device.get(), image, &requirements);
    const VkMemoryDedicatedAllocateInfo dedicated{
        .sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO,
        .image = image,
    };
    const VkImportMemoryFdInfoKHR imported{
        .sType = VK_STRUCTURE_TYPE_IMPORT_MEMORY_FD_INFO_KHR,
        .pNext = &dedicated,
        .handleType = shared_handle_type,
        .fd = static_cast<int>(memory.get()),
    };
    const VkMemoryAllocateInfo allocation{
        .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
        .pNext = &imported,
        .allocationSize = allocation_bytes,
        .memoryTypeIndex = renderer::find_memory_type(
            device.physical(), requirements.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
    };
    renderer::DeviceHandle<VkDeviceMemory> allocated = renderer::create_device_handle<VkDeviceMemory>(
        device.get(), allocation, vkAllocateMemory, vkFreeMemory, "importing the memory of a shared attachment");
    memory.release();
    return allocated;
}

}
