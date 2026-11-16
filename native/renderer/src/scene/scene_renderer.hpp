#pragma once

#include <vector>

#include "draw/batch_renderer.hpp"
#include "graph/render_graph.hpp"
#include "scene/scene_frame.hpp"
#include "terrain/terrain_renderer.hpp"
#include "terrain/terrain_sections.hpp"

namespace minemod::renderer {

struct SceneTargets {
    ImageId color;
    ImageId depth;
    std::vector<ImageId> sampled_textures;
};

class SceneRenderer {
public:
    SceneRenderer(const TerrainRenderer& terrain, BatchRenderer& batches) noexcept
        : terrain_(terrain), batches_(batches) {}

    void add_passes(
        RenderGraph& graph, const SceneTargets& targets, const SceneFrame& frame, const TerrainSections& sections);

private:
    const TerrainRenderer& terrain_;
    BatchRenderer& batches_;
};

}
