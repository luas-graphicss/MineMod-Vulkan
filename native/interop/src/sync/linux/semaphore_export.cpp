#include "sync/semaphore_export.hpp"

#include "device/vulkan_error.hpp"
#include "sync/shared_semaphore.hpp"

namespace minemod::interop {

const VkExternalSemaphoreHandleTypeFlagBits shared_semaphore_type = VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_FD_BIT;

const std::array<const char*, 1> shared_semaphore_extensions{VK_KHR_EXTERNAL_SEMAPHORE_FD_EXTENSION_NAME};

SemaphoreExport::SemaphoreExport(VkDevice device)
    : device_(device), export_semaphore_(vkGetDeviceProcAddr(device, "vkGetSemaphoreFdKHR")) {
    if (export_semaphore_ == nullptr) {
        throw renderer::UnsupportedSystem(
            "the device was created without " VK_KHR_EXTERNAL_SEMAPHORE_FD_EXTENSION_NAME
            ", so it cannot share semaphores with the OpenGL provider");
    }
}

ExportedHandle SemaphoreExport::export_handle(VkSemaphore semaphore) const {
    const VkSemaphoreGetFdInfoKHR request{
        .sType = VK_STRUCTURE_TYPE_SEMAPHORE_GET_FD_INFO_KHR,
        .semaphore = semaphore,
        .handleType = shared_semaphore_type,
    };
    int descriptor = -1;
    const auto export_semaphore = reinterpret_cast<PFN_vkGetSemaphoreFdKHR>(export_semaphore_);
    renderer::require_success(export_semaphore(device_, &request, &descriptor), "exporting a shared semaphore");
    return ExportedHandle(descriptor);
}

}
