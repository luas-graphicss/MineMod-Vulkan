#include "memory/exportable_memory.hpp"

#include "memory/external_support.hpp"
#include "resources/memory_type.hpp"

namespace minemod::interop {

renderer::DeviceHandle<VkDeviceMemory> allocate_exportable_memory(
    const renderer::Device& device,
    const VkMemoryRequirements& requirements,
    const VkMemoryDedicatedAllocateInfo& dedicated_to) {
    const VkExportMemoryAllocateInfo exported{
        .sType = VK_STRUCTURE_TYPE_EXPORT_MEMORY_ALLOCATE_INFO,
        .pNext = &dedicated_to,
        .handleTypes = shared_handle_type,
    };
    const VkMemoryAllocateInfo allocation{
        .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
        .pNext = &exported,
        .allocationSize = requirements.size,
        .memoryTypeIndex = renderer::find_memory_type(
            device.physical(), requirements.memoryTypeBits, VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
    };
    return renderer::create_device_handle<VkDeviceMemory>(
        device.get(), allocation, vkAllocateMemory, vkFreeMemory, "allocating shared memory");
}

}
