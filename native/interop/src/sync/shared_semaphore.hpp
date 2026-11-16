#pragma once

#include <array>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "memory/owned_descriptor.hpp"

namespace minemod::interop {

inline constexpr VkExternalSemaphoreHandleTypeFlagBits shared_semaphore_type =
    VK_EXTERNAL_SEMAPHORE_HANDLE_TYPE_OPAQUE_FD_BIT;

inline constexpr std::array<const char*, 1> shared_semaphore_extensions{VK_KHR_EXTERNAL_SEMAPHORE_FD_EXTENSION_NAME};

bool can_export_semaphores(VkPhysicalDevice device);

class SharedSemaphore {
public:
    explicit SharedSemaphore(const renderer::Device& device);

    VkSemaphore get() const noexcept {
        return semaphore_.get();
    }

    OwnedDescriptor export_descriptor() const;

private:
    VkDevice device_;
    PFN_vkGetSemaphoreFdKHR get_semaphore_descriptor_;
    renderer::DeviceHandle<VkSemaphore> semaphore_;
};

}
