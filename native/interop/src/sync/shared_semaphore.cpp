#include "sync/shared_semaphore.hpp"

#include "device/vulkan_error.hpp"

namespace minemod::interop {

bool can_export_semaphores(VkPhysicalDevice device) {
    const VkPhysicalDeviceExternalSemaphoreInfo request{
        .sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_SEMAPHORE_INFO,
        .handleType = shared_semaphore_type,
    };
    VkExternalSemaphoreProperties answer{.sType = VK_STRUCTURE_TYPE_EXTERNAL_SEMAPHORE_PROPERTIES};
    vkGetPhysicalDeviceExternalSemaphoreProperties(device, &request, &answer);
    return (answer.externalSemaphoreFeatures & VK_EXTERNAL_SEMAPHORE_FEATURE_EXPORTABLE_BIT) != 0;
}

SharedSemaphore::SharedSemaphore(const renderer::Device& device) : semaphore_export_(device.get()) {
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
        device.get(), create_info, vkCreateSemaphore, vkDestroySemaphore, "creating a shared semaphore");
}

ExportedHandle SharedSemaphore::export_handle() const {
    return semaphore_export_.export_handle(semaphore_.get());
}

}
