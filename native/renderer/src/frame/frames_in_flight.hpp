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
    Frame submit_part(const SubmitSemaphores& semaphores = {});
    void wait_for_submitted_parts();
    void submit(const SubmitSemaphores& semaphores = {});

    std::optional<std::uint64_t> completed_frame() const noexcept {
        return completed_frame_;
    }

private:
    struct Part {
        VkCommandBuffer commands = VK_NULL_HANDLE;
        DeviceHandle<VkFence> submitted;
    };

    struct Slot {
        DeviceHandle<VkCommandPool> pool;
        std::vector<Part> parts;
        std::size_t submitted_parts = 0;
        std::optional<std::uint64_t> pending_frame;
    };

    static Slot create_slot(const Device& device);
    static Part create_part(const Device& device, VkCommandPool pool);

    Slot& next_slot() noexcept {
        return slots_[next_frame_ % slots_.size()];
    }

    Slot& recording_slot();
    void wait_for_parts_of(const Slot& slot) const;
    void recycle(Slot& slot);
    void enqueue_part(Slot& slot, const SubmitSemaphores& semaphores);

    const Device& device_;
    std::vector<Slot> slots_;
    std::uint64_t next_frame_ = 0;
    std::optional<std::uint64_t> completed_frame_;
    bool is_recording_ = false;
};

}
