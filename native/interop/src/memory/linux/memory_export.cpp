#include "memory/exportable_memory.hpp"

#include "device/vulkan_error.hpp"
#include "memory/external_support.hpp"
#include "memory/shared_image.hpp"

namespace minemod::interop {

const VkExternalMemoryHandleTypeFlagBits shared_handle_type = VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT;

const std::array<const char*, 1> shared_image_extensions{VK_KHR_EXTERNAL_MEMORY_FD_EXTENSION_NAME};

MemoryExport::MemoryExport(VkDevice device)
    : device_(device), export_memory_(vkGetDeviceProcAddr(device, "vkGetMemoryFdKHR")) {
    if (export_memory_ == nullptr) {
        throw renderer::UnsupportedSystem(
            "the device was created without " VK_KHR_EXTERNAL_MEMORY_FD_EXTENSION_NAME
            ", so it cannot share memory with the OpenGL provider");
    }
}

ExportedHandle MemoryExport::export_handle(VkDeviceMemory memory) const {
    const VkMemoryGetFdInfoKHR request{
        .sType = VK_STRUCTURE_TYPE_MEMORY_GET_FD_INFO_KHR,
        .memory = memory,
        .handleType = shared_handle_type,
    };
    int descriptor = -1;
    const auto export_memory = reinterpret_cast<PFN_vkGetMemoryFdKHR>(export_memory_);
    renderer::require_success(export_memory(device_, &request, &descriptor), "exporting shared memory");
    return ExportedHandle(descriptor);
}

}
