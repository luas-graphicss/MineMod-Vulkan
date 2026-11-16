#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"

namespace minemod::renderer {

class FramesInFlight {
public:
    struct Frame {
        std::uint64_t number;
        VkCommandBuffer commands;
    };

    FramesInFlight(const Device& device, std::size_t frame_count);
    FramesInFlight(const FramesInFlight&) = delete;
    FramesInFlight& operator=(const FramesInFlight&) = delete;
    ~FramesInFlight();

    Frame begin();
    void submit(const SubmitSemaphores& semaphores = {});

    std::optional<std::uint64_t> completed_frame() const noexcept {
        return completed_frame_;
    }

private:
    struct Slot {
        DeviceHandle<VkCommandPool> pool;
        VkCommandBuffer commands = VK_NULL_HANDLE;
        DeviceHandle<VkFence> submitted;
        std::optional<std::uint64_t> pending_frame;
    };

    static Slot create_slot(const Device& device);

    Slot& next_slot() noexcept {
        return slots_[next_frame_ % slots_.size()];
    }

    const Device& device_;
    std::vector<Slot> slots_;
    std::uint64_t next_frame_ = 0;
    std::optional<std::uint64_t> completed_frame_;
    bool is_recording_ = false;
};

}
