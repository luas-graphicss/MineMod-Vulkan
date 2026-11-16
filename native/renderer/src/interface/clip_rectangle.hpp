#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

struct LowerLeftRectangle {
    std::int32_t left;
    std::int32_t bottom;
    std::int32_t width;
    std::int32_t height;
};

VkRect2D clip_rectangle(const LowerLeftRectangle& rectangle, VkExtent2D frame) noexcept;

}
