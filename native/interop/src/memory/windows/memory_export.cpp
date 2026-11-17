#include "memory/exportable_memory.hpp"

#include "device/vulkan_error.hpp"
#include "memory/external_support.hpp"
#include "memory/shared_image.hpp"

#include <windows.h>

#include <vulkan/vulkan_win32.h>

namespace minemod::interop {

const VkExternalMemoryHandleTypeFlagBits shared_handle_type = VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_WIN32_BIT;

const std::array<const char*, 1> shared_image_extensions{VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME};

MemoryExport::MemoryExport(VkDevice device)
    : device_(device), export_memory_(vkGetDeviceProcAddr(device, "vkGetMemoryWin32HandleKHR")) {
    if (export_memory_ == nullptr) {
        throw renderer::UnsupportedSystem(
            "the device was created without " VK_KHR_EXTERNAL_MEMORY_WIN32_EXTENSION_NAME
            ", so it cannot share memory with the OpenGL provider");
    }
}

ExportedHandle MemoryExport::export_handle(VkDeviceMemory memory) const {
    const VkMemoryGetWin32HandleInfoKHR request{
        .sType = VK_STRUCTURE_TYPE_MEMORY_GET_WIN32_HANDLE_INFO_KHR,
        .memory = memory,
        .handleType = shared_handle_type,
    };
    HANDLE handle = nullptr;
    const auto export_memory = reinterpret_cast<PFN_vkGetMemoryWin32HandleKHR>(export_memory_);
    renderer::require_success(export_memory(device_, &request, &handle), "exporting shared memory");
    return ExportedHandle(reinterpret_cast<std::intptr_t>(handle));
}

}
