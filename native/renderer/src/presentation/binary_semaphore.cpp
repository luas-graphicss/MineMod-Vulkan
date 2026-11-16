#include "presentation/binary_semaphore.hpp"

namespace minemod::renderer {

DeviceHandle<VkSemaphore> create_binary_semaphore(VkDevice device) {
    const VkSemaphoreCreateInfo create_info{.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
    return create_device_handle<VkSemaphore>(
        device, create_info, vkCreateSemaphore, vkDestroySemaphore, "creating a presentation semaphore");
}

}
