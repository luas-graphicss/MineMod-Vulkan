#pragma once

#include <span>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

enum class PresentPacing { vsync, immediate };

VkPresentModeKHR choose_present_mode(std::span<const VkPresentModeKHR> supported, PresentPacing pacing) noexcept;

}
