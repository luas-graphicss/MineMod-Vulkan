#pragma once

#include <array>
#include <cstdint>
#include <vector>

#include <vulkan/vulkan.h>

namespace minemod::scene::test_support {

using Color = std::array<std::uint8_t, 4>;

inline constexpr float interface_depth = 0.5f;

std::array<float, 16> pixel_projection(VkExtent2D extent);
std::vector<std::uint8_t> colored_rectangle(const VkRect2D& pixels, const Color& color);
std::vector<std::uint8_t> colored_rectangle_at(const VkRect2D& pixels, float depth, const Color& color);
std::vector<std::uint8_t> textured_rectangle(const VkRect2D& pixels, const Color& color);
std::vector<std::uint8_t> solid_texels(VkExtent2D extent, const Color& color);

}
