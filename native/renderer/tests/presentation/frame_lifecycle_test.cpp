#include <stdexcept>

#include <doctest.h>

#include "device/vulkan_error.hpp"
#include "presentation/frame_lifecycle.hpp"

namespace {

using namespace minemod::renderer;

constexpr VkExtent2D window_extent{854, 480};
constexpr VkExtent2D minimized_extent{0, 0};

FrameLifecycle running_lifecycle() {
    FrameLifecycle lifecycle(window_extent);
    lifecycle.swapchain_created();
    return lifecycle;
}

}

TEST_CASE("a visible surface waits for its swapchain before frames run") {
    FrameLifecycle lifecycle(window_extent);
    CHECK(lifecycle.state() == FrameState::awaiting_swapchain);

    lifecycle.swapchain_created();

    CHECK(lifecycle.state() == FrameState::running);
    CHECK(lifecycle.acquired(VK_SUCCESS) == AcquiredFrame::draw);
}

TEST_CASE("a surface without area suspends the frame loop until it has area again") {
    FrameLifecycle lifecycle = running_lifecycle();

    lifecycle.resize(minimized_extent);
    CHECK(lifecycle.state() == FrameState::suspended);

    lifecycle.resize(window_extent);
    CHECK(lifecycle.state() == FrameState::awaiting_swapchain);
}

TEST_CASE("a resize to the current extent keeps the swapchain") {
    FrameLifecycle lifecycle = running_lifecycle();

    lifecycle.resize(window_extent);

    CHECK(lifecycle.state() == FrameState::running);
}

TEST_CASE("a suboptimal image is drawn and the swapchain is replaced after it is presented") {
    FrameLifecycle lifecycle = running_lifecycle();

    CHECK(lifecycle.acquired(VK_SUBOPTIMAL_KHR) == AcquiredFrame::draw);
    CHECK(lifecycle.state() == FrameState::running);

    lifecycle.presented(VK_SUCCESS);
    CHECK(lifecycle.state() == FrameState::awaiting_swapchain);
}

TEST_CASE("an out-of-date swapchain skips the frame and is replaced before the next one") {
    FrameLifecycle lifecycle = running_lifecycle();

    CHECK(lifecycle.acquired(VK_ERROR_OUT_OF_DATE_KHR) == AcquiredFrame::skip);
    CHECK(lifecycle.state() == FrameState::awaiting_swapchain);
    CHECK_THROWS_AS(lifecycle.acquired(VK_SUCCESS), std::logic_error);
}

TEST_CASE("an out-of-date result while presenting replaces the swapchain") {
    FrameLifecycle lifecycle = running_lifecycle();
    lifecycle.acquired(VK_SUCCESS);

    lifecycle.presented(VK_ERROR_OUT_OF_DATE_KHR);

    CHECK(lifecycle.state() == FrameState::awaiting_swapchain);
}

TEST_CASE("a lost device ends the frame loop instead of replacing the swapchain") {
    FrameLifecycle lifecycle = running_lifecycle();

    CHECK(lifecycle.acquired(VK_ERROR_DEVICE_LOST) == AcquiredFrame::skip);
    lifecycle.resize(VkExtent2D{1280, 720});

    CHECK(lifecycle.state() == FrameState::device_lost);
    CHECK_THROWS_AS(lifecycle.swapchain_created(), std::logic_error);
}

TEST_CASE("a frame presented after the surface was resized keeps the pending state") {
    FrameLifecycle lifecycle = running_lifecycle();
    lifecycle.acquired(VK_SUCCESS);
    lifecycle.resize(minimized_extent);

    lifecycle.presented(VK_SUCCESS);

    CHECK(lifecycle.state() == FrameState::suspended);
}

TEST_CASE("a stopped frame loop ignores later surface and device events") {
    FrameLifecycle lifecycle = running_lifecycle();

    lifecycle.stop();
    lifecycle.resize(minimized_extent);
    lifecycle.presented(VK_ERROR_DEVICE_LOST);

    CHECK(lifecycle.state() == FrameState::stopped);
}

TEST_CASE("an unexpected presentation result is reported as a Vulkan error") {
    FrameLifecycle lifecycle = running_lifecycle();

    CHECK_THROWS_AS(lifecycle.acquired(VK_ERROR_SURFACE_LOST_KHR), VulkanError);
    CHECK_THROWS_AS(lifecycle.presented(VK_ERROR_OUT_OF_HOST_MEMORY), VulkanError);
}
