#pragma once

#include <array>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "memory/exported_handle.hpp"
#include "sync/semaphore_export.hpp"

namespace minemod::interop {

extern const VkExternalSemaphoreHandleTypeFlagBits shared_semaphore_type;

extern const std::array<const char*, 1> shared_semaphore_extensions;

bool can_export_semaphores(VkPhysicalDevice device);

class SharedSemaphore {
public:
    explicit SharedSemaphore(const renderer::Device& device);

    VkSemaphore get() const noexcept {
        return semaphore_.get();
    }

    ExportedHandle export_handle() const;

private:
    SemaphoreExport semaphore_export_;
    renderer::DeviceHandle<VkSemaphore> semaphore_;
};

}
