#include "presentation/frame_lifecycle.hpp"

#include <stdexcept>

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

namespace {

FrameState state_for(VkExtent2D surface_extent) noexcept {
    const bool has_area = surface_extent.width != 0 && surface_extent.height != 0;
    return has_area ? FrameState::awaiting_swapchain : FrameState::suspended;
}

}

FrameLifecycle::FrameLifecycle(VkExtent2D surface_extent) noexcept
    : state_(state_for(surface_extent)), surface_extent_(surface_extent) {}

void FrameLifecycle::resize(VkExtent2D surface_extent) noexcept {
    const bool is_unchanged =
        surface_extent.width == surface_extent_.width && surface_extent.height == surface_extent_.height;
    if (has_ended() || is_unchanged) {
        return;
    }
    surface_extent_ = surface_extent;
    state_ = state_for(surface_extent);
}

void FrameLifecycle::swapchain_created() {
    if (state_ != FrameState::awaiting_swapchain) {
        throw std::logic_error("a swapchain is only created while the frame loop awaits one");
    }
    state_ = FrameState::running;
    is_suboptimal_ = false;
}

AcquiredFrame FrameLifecycle::acquired(VkResult result) {
    if (state_ != FrameState::running) {
        throw std::logic_error("a swapchain image is only acquired while the frame loop runs");
    }
    switch (result) {
        case VK_SUCCESS:
            return AcquiredFrame::draw;
        case VK_SUBOPTIMAL_KHR:
            is_suboptimal_ = true;
            return AcquiredFrame::draw;
        case VK_ERROR_OUT_OF_DATE_KHR:
            state_ = FrameState::awaiting_swapchain;
            return AcquiredFrame::skip;
        case VK_ERROR_DEVICE_LOST:
            state_ = FrameState::device_lost;
            return AcquiredFrame::skip;
        default:
            throw VulkanError("acquiring a swapchain image", result);
    }
}

void FrameLifecycle::presented(VkResult result) {
    if (result == VK_ERROR_DEVICE_LOST) {
        if (state_ != FrameState::stopped) {
            state_ = FrameState::device_lost;
        }
        return;
    }
    const bool is_outdated = result == VK_SUBOPTIMAL_KHR || result == VK_ERROR_OUT_OF_DATE_KHR;
    if (result != VK_SUCCESS && !is_outdated) {
        throw VulkanError("presenting a swapchain image", result);
    }
    if ((is_outdated || is_suboptimal_) && state_ == FrameState::running) {
        state_ = FrameState::awaiting_swapchain;
    }
}

void FrameLifecycle::stop() noexcept {
    state_ = FrameState::stopped;
}

bool FrameLifecycle::has_ended() const noexcept {
    return state_ == FrameState::device_lost || state_ == FrameState::stopped;
}

}
