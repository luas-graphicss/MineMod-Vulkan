#include "scene/scene_renderer.hpp"

#include <array>
#include <optional>
#include <utility>

#include "frame/frame_rendering.hpp"
#include "graph/graph_images.hpp"
#include "graph/image_use.hpp"
#include "terrain/terrain_layer.hpp"

namespace minemod::renderer {
namespace {

std::vector<ImageAccess> stage_accesses(const SceneTargets& targets) {
    std::vector<ImageAccess> accesses{
        {targets.color, uses::color_attachment},
        {targets.depth, uses::depth_attachment},
    };
    for (const ImageId texture : targets.sampled_textures) {
        accesses.push_back({texture, uses::sampled_by_shaders});
    }
    return accesses;
}

FrameAttachments attachments(const SceneTargets& targets, const GraphImages& images) {
    return FrameAttachments{images.at(targets.color), images.at(targets.depth)};
}

}

void SceneRenderer::add_passes(
    RenderGraph& graph, const SceneTargets& targets, const SceneFrame& frame, const TerrainSections& sections) {
    const std::vector<ImageAccess> accesses = stage_accesses(targets);
    std::optional<PassId> previous;
    const auto add_stage = [&](const char* name, RecordPass record) {
        const PassId stage = graph.add_pass(name, accesses, std::move(record));
        if (previous) {
            graph.add_dependency(stage, *previous);
        }
        previous = stage;
    };

    if (frame.world) {
        add_stage("world", [this, targets, frame, &sections](VkCommandBuffer commands, const GraphImages& images) {
            const WorldStage& world = *frame.world;
            const BatchPass pass{frame.extent, world.view};
            begin_frame_rendering(commands, attachments(targets, images), frame.clear_color);
            batches_.record(commands, pass, world.sky);
            terrain_.record(commands, world.terrain, sections, opaque_terrain_layers);
            batches_.record(commands, pass, world.solid);
            terrain_.record(commands, world.terrain, sections, translucent_terrain_layers);
            batches_.record(commands, pass, world.blended);
            vkCmdEndRendering(commands);
        });
    }
    if (frame.world && !frame.hand.batches.empty()) {
        add_stage("hand", [this, targets, frame](VkCommandBuffer commands, const GraphImages& images) {
            begin_frame_rendering(commands, attachments(targets, images), std::nullopt);
            batches_.record(commands, {frame.extent, frame.hand.view}, frame.hand.batches);
            vkCmdEndRendering(commands);
        });
    }
    add_stage("interface", [this, targets, frame](VkCommandBuffer commands, const GraphImages& images) {
        const std::optional<std::array<float, 4>> clear_color =
            frame.world ? std::nullopt : std::optional(frame.clear_color);
        begin_frame_rendering(commands, attachments(targets, images), clear_color);
        batches_.record(commands, {frame.extent, frame.interface.view}, frame.interface.batches);
        vkCmdEndRendering(commands);
    });
}

}
