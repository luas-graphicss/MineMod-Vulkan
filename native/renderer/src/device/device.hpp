#pragma once

#include <functional>
#include <span>

#include <vulkan/vulkan.h>

#include "device/device_handle.hpp"
#include "device/physical_device.hpp"

namespace minemod::renderer {

struct SubmitSemaphores {
    VkSemaphore wait = VK_NULL_HANDLE;
    VkPipelineStageFlags2 wait_stage = VK_PIPELINE_STAGE_2_NONE;
    VkSemaphore signal = VK_NULL_HANDLE;
};

class Device {
public:
    Device(const SelectedDevice& selected, std::span<const char* const> extensions);

    VkDevice get() const noexcept {
        return logical_.handle;
    }

    VkPhysicalDevice physical() const noexcept {
        return physical_;
    }

    VkQueue queue() const noexcept {
        return queue_;
    }

    void submit(const std::function<void(VkCommandBuffer)>& record, const SubmitSemaphores& semaphores = {}) const;

private:
    struct LogicalDevice {
        LogicalDevice() = default;
        LogicalDevice(const LogicalDevice&) = delete;
        LogicalDevice& operator=(const LogicalDevice&) = delete;
        ~LogicalDevice();

        VkDevice handle = VK_NULL_HANDLE;
    };

    VkPhysicalDevice physical_;
    LogicalDevice logical_;
    VkQueue queue_ = VK_NULL_HANDLE;
    DeviceHandle<VkCommandPool> command_pool_;
};

}
