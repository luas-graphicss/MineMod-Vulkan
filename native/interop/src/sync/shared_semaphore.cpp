#include "sync/shared_semaphore.hpp"

#include "device/vulkan_error.hpp"

namespace minemod::interop {
namespace {

PFN_vkGetSemaphoreFdKHR find_semaphore_export(VkDevice device) {
    const auto export_semaphore =
        reinterpret_cast<PFN_vkGetSemaphoreFdKHR>(vkGetDeviceProcAddr(device, "vkGetSemaphoreFdKHR"));
    if (export_semaphore == nullptr) {
        throw renderer::UnsupportedSystem(
            "the device was created without " VK_KHR_EXTERNAL_SEMAPHORE_FD_EXTENSION_NAME
            ", so it cannot share semaphores with the OpenGL provider");
    }
    return export_semaphore;
}

}

bool can_export_semaphores(VkPhysicalDevice device) {
    const VkPhysicalDeviceExternalSemaphoreInfo request{
        .sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_SEMAPHORE_INFO,
        .handleType = shared_semaphore_type,
    };
    VkExternalSemaphoreProperties answer{.sType = VK_STRUCTURE_TYPE_EXTERNAL_SEMAPHORE_PROPERTIES};
    vkGetPhysicalDeviceExternalSemaphoreProperties(device, &request, &answer);
    return (answer.externalSemaphoreFeatures & VK_EXTERNAL_SEMAPHORE_FEATURE_EXPORTABLE_BIT) != 0;
}

SharedSemaphore::SharedSemaphore(const renderer::Device& device)
    : device_(device.get()), get_semaphore_descriptor_(find_semaphore_export(device.get())) {
    if (!can_export_semaphores(device.physical())) {
        throw renderer::UnsupportedSystem("the device cannot export semaphores to the OpenGL provider");
    }

    const VkExportSemaphoreCreateInfo exported{
        .sType = VK_STRUCTURE_TYPE_EXPORT_SEMAPHORE_CREATE_INFO,
        .handleTypes = shared_semaphore_type,
    };
    const VkSemaphoreCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO,
        .pNext = &exported,
    };
    semaphore_ = renderer::create_device_handle<VkSemaphore>(
        device_, create_info, vkCreateSemaphore, vkDestroySemaphore, "creating a shared semaphore");
}

OwnedDescriptor SharedSemaphore::export_descriptor() const {
    const VkSemaphoreGetFdInfoKHR request{
        .sType = VK_STRUCTURE_TYPE_SEMAPHORE_GET_FD_INFO_KHR,
        .semaphore = semaphore_.get(),
        .handleType = shared_semaphore_type,
    };
    int descriptor = -1;
    renderer::require_success(
        get_semaphore_descriptor_(device_, &request, &descriptor), "exporting a shared semaphore");
    return OwnedDescriptor(descriptor);
}

}
