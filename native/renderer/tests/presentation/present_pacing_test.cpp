#include <array>

#include <doctest.h>
#include <vulkan/vulkan.h>

#include "presentation/present_pacing.hpp"

namespace {

using namespace minemod::renderer;

}

TEST_CASE("vertical sync presents in the mode every surface supports") {
    constexpr std::array supported{
        VK_PRESENT_MODE_IMMEDIATE_KHR,
        VK_PRESENT_MODE_MAILBOX_KHR,
        VK_PRESENT_MODE_FIFO_KHR,
    };

    CHECK(choose_present_mode(supported, PresentPacing::vsync) == VK_PRESENT_MODE_FIFO_KHR);
}

TEST_CASE("unpaced presentation shows each frame as soon as it is ready") {
    constexpr std::array supported{
        VK_PRESENT_MODE_FIFO_KHR,
        VK_PRESENT_MODE_MAILBOX_KHR,
        VK_PRESENT_MODE_IMMEDIATE_KHR,
    };

    CHECK(choose_present_mode(supported, PresentPacing::immediate) == VK_PRESENT_MODE_IMMEDIATE_KHR);
}

TEST_CASE("unpaced presentation replaces the queued frame on a surface that cannot tear") {
    constexpr std::array supported{VK_PRESENT_MODE_FIFO_KHR, VK_PRESENT_MODE_MAILBOX_KHR};

    CHECK(choose_present_mode(supported, PresentPacing::immediate) == VK_PRESENT_MODE_MAILBOX_KHR);
}

TEST_CASE("unpaced presentation falls back to vertical sync on a surface that only queues frames") {
    constexpr std::array supported{VK_PRESENT_MODE_FIFO_KHR, VK_PRESENT_MODE_FIFO_RELAXED_KHR};

    CHECK(choose_present_mode(supported, PresentPacing::immediate) == VK_PRESENT_MODE_FIFO_KHR);
}
