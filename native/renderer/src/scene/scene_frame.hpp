#pragma once

#include <array>
#include <optional>
#include <span>

#include <vulkan/vulkan.h>

#include "draw/batch_renderer.hpp"
#include "terrain/terrain_frame.hpp"

namespace minemod::renderer {

struct WorldStage {
    VkDescriptorSet view;
    TerrainFrame terrain;
    std::span<const DrawBatch> sky;
    std::span<const DrawBatch> solid;
    std::span<const DrawBatch> blended;
};

struct OverlayStage {
    VkDescriptorSet view;
    std::span<const DrawBatch> batches;
};

struct SceneFrame {
    VkExtent2D extent;
    std::array<float, 4> clear_color;
    std::optional<WorldStage> world;
    OverlayStage hand;
    OverlayStage interface;
};

}
