#include "memory/exportable_memory.hpp"

#include "device/vulkan_error.hpp"
#include "memory/external_support.hpp"
#include "resources/memory_type.hpp"

namespace minemod::interop {
namespace {

PFN_vkGetMemoryFdKHR find_memory_export(VkDevice device) {
    const auto export_memory =
        reinterpret_cast<PFN_vkGetMemoryFdKHR>(vkGetDeviceProcAddr(device, "vkGetMemoryFdKHR"));
    if (export_memory == nullptr) {
        throw renderer::UnsupportedSystem(
            "the device was created without " VK_KHR_EXTERNAL_MEMORY_FD_EXTENSION_NAME
            ", so it cannot share memory with the OpenGL provider");
    }
    return export_memory;
}

}

MemoryExport::MemoryExport(VkDevice device) : device_(device), get_memory_descriptor_(find_memory_export(device)) {}

OwnedDescriptor MemoryExport::export_descriptor(VkDeviceMemory memory) const {
    const VkMemoryGetFdInfoKHR request{
        .sType = VK_STRUCTURE_TYPE_MEMORY_GET_FD_INFO_KHR,
        .memory = memory,
        .handleType = shared_handle_type,
    };
    int descriptor = -1;
    renderer::require_success(get_memory_descriptor_(device_, &request, &descriptor), "exporting shared memory");
    return OwnedDescriptor(descriptor);
}

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
