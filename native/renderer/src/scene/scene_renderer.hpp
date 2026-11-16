#pragma once

#include <cstdint>
#include <optional>
#include <string_view>
#include <vector>

#include "draw/batch_renderer.hpp"
#include "effects/frame_blur.hpp"
#include "graph/render_graph.hpp"
#include "scene/scene_frame.hpp"
#include "terrain/terrain_renderer.hpp"
#include "terrain/terrain_sections.hpp"

namespace minemod::renderer {

struct SceneTargets {
    ImageId color;
    ImageId depth;
    std::vector<ImageId> sampled_textures;
    std::optional<ImageId> scratch{};
};

inline constexpr std::string_view world_pass = "world";
inline constexpr std::string_view hand_pass = "hand";
inline constexpr std::string_view backdrop_pass = "backdrop";
inline constexpr std::string_view blur_pass = "blur";
inline constexpr std::string_view interface_pass = "interface";

class SceneRenderer {
public:
    SceneRenderer(const TerrainRenderer& terrain, BatchRenderer& batches, const FrameBlurRenderer& blur) noexcept
        : terrain_(terrain), batches_(batches), blur_(blur) {}

    std::optional<SceneStage> add_passes(
        RenderGraph& graph, const SceneTargets& targets, const SceneFrame& frame, const TerrainSections& sections);

    std::uint32_t recorded_terrain_draws() const noexcept {
        return recorded_terrain_draws_;
    }

private:
    const TerrainRenderer& terrain_;
    BatchRenderer& batches_;
    const FrameBlurRenderer& blur_;
    std::uint32_t recorded_terrain_draws_ = 0;
};

}
