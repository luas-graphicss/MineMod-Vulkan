#pragma once

#include <vulkan/vulkan.h>

#include "textures/sampler_cache.hpp"

namespace minemod::renderer {

inline constexpr VkExtent2D lightmap_extent{16, 16};

inline constexpr SamplerDescription lightmap_sampler{
    VK_FILTER_LINEAR,
    VK_FILTER_LINEAR,
    VK_SAMPLER_MIPMAP_MODE_NEAREST,
    VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
    1,
};

}
