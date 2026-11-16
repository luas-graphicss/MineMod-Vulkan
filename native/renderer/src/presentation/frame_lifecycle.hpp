#pragma once

#include <vulkan/vulkan.h>

namespace minemod::renderer {

enum class FrameState { awaiting_swapchain, running, suspended, device_lost, stopped };

enum class AcquiredFrame { draw, skip };

class FrameLifecycle {
public:
    explicit FrameLifecycle(VkExtent2D surface_extent) noexcept;

    FrameState state() const noexcept {
        return state_;
    }

    void resize(VkExtent2D surface_extent) noexcept;
    void swapchain_created();
    void swapchain_outdated() noexcept;
    AcquiredFrame acquired(VkResult result);
    void presented(VkResult result);
    void stop() noexcept;

private:
    bool has_ended() const noexcept;

    FrameState state_;
    VkExtent2D surface_extent_;
    bool is_suboptimal_ = false;
};

}
