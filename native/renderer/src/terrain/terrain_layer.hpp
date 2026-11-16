#pragma once

#include <array>
#include <cstddef>
#include <cstdint>

#include <vulkan/vulkan.h>

#include "pipelines/graphics_pipeline_state.hpp"

namespace minemod::renderer {

enum class TerrainLayer : std::uint8_t { solid, cutout_mipped, cutout, translucent };

inline constexpr std::array terrain_layers{
    TerrainLayer::solid,
    TerrainLayer::cutout_mipped,
    TerrainLayer::cutout,
    TerrainLayer::translucent,
};

inline constexpr std::size_t terrain_layer_count = terrain_layers.size();

enum class DrawDirection { front_to_back, back_to_front };
enum class AtlasLevels { mipmapped, base_only };

struct LayerState {
    DepthState depth;
    Blending blending;
    float alpha_cutoff;
    AtlasLevels atlas_levels;
    DrawDirection direction;
};

inline constexpr DepthState terrain_depth{true, true, VK_COMPARE_OP_LESS_OR_EQUAL};

constexpr std::size_t layer_index(TerrainLayer layer) noexcept {
    return static_cast<std::size_t>(layer);
}

constexpr LayerState layer_state(TerrainLayer layer) noexcept {
    switch (layer) {
    case TerrainLayer::cutout_mipped:
        return {terrain_depth, Blending::disabled, 0.5f, AtlasLevels::mipmapped, DrawDirection::front_to_back};
    case TerrainLayer::cutout:
        return {terrain_depth, Blending::disabled, 0.1f, AtlasLevels::base_only, DrawDirection::front_to_back};
    case TerrainLayer::translucent:
        return {terrain_depth, Blending::alpha, 0.0f, AtlasLevels::mipmapped, DrawDirection::back_to_front};
    case TerrainLayer::solid:
        break;
    }
    return {terrain_depth, Blending::disabled, 0.0f, AtlasLevels::mipmapped, DrawDirection::front_to_back};
}

}
