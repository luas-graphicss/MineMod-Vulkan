#pragma once

#include <cstdint>
#include <optional>
#include <span>
#include <vector>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

struct SpriteRegion {
    std::uint32_t x;
    std::uint32_t y;
    std::uint32_t width;
    std::uint32_t height;

    friend bool operator==(const SpriteRegion&, const SpriteRegion&) = default;
};

struct AtlasLayout {
    VkExtent2D extent;
    std::vector<SpriteRegion> regions;
};

struct AtlasLimits {
    std::uint32_t max_extent;
    std::uint32_t mip_levels;
};

std::optional<AtlasLayout> pack_atlas(std::span<const VkExtent2D> sprites, const AtlasLimits& limits);

}
