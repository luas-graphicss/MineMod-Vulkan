#include "presentation/present_pacing.hpp"

#include <algorithm>
#include <array>

namespace minemod::renderer {

namespace {

constexpr std::array unpaced_preferences{VK_PRESENT_MODE_IMMEDIATE_KHR, VK_PRESENT_MODE_MAILBOX_KHR};

}

VkPresentModeKHR choose_present_mode(std::span<const VkPresentModeKHR> supported, PresentPacing pacing) noexcept {
    if (pacing == PresentPacing::vsync) {
        return VK_PRESENT_MODE_FIFO_KHR;
    }
    const auto preferred = std::ranges::find_if(unpaced_preferences, [&](VkPresentModeKHR mode) {
        return std::ranges::find(supported, mode) != supported.end();
    });
    return preferred == unpaced_preferences.end() ? VK_PRESENT_MODE_FIFO_KHR : *preferred;
}

}
