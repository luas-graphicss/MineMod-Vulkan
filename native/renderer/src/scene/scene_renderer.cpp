#include "scene/scene_renderer.hpp"

#include <array>
#include <optional>
#include <string>
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
    const auto add_pass = [&](std::string_view name, std::vector<ImageAccess> used, RecordPass record) {
        const PassId pass = graph.add_pass(std::string(name), std::move(used), std::move(record));
        if (previous) {
            graph.add_dependency(pass, *previous);
        }
        previous = pass;
    };
    const auto add_stage = [&](std::string_view name, RecordPass record) {
        add_pass(name, accesses, std::move(record));
    };
    const auto add_blur = [&](ImageId source, VkDescriptorSet bound_source, ImageId target, BlurAxis axis) {
        const float radius = frame.backdrop->blur.radius;
        add_pass(
            blur_pass,
            {{source, uses::sampled_by_fragment_shader}, {target, uses::color_attachment}},
            [this, bound_source, target, axis, radius](VkCommandBuffer commands, const GraphImages& images) {
                blur_.record(commands, bound_source, images.at(target), axis, radius);
            });
    };
    if (frame.world) {
        add_stage(world_pass, [this, targets, frame, &sections](VkCommandBuffer commands, const GraphImages& images) {
            const WorldStage& world = *frame.world;
            const BatchPass pass{frame.extent, world.view};
            begin_frame_rendering(commands, attachments(targets, images), frame.clear_color);
            batches_.record(commands, pass, world.sky);
            recorded_terrain_draws_ = terrain_.record(commands, world.terrain, sections, opaque_terrain_layers);
            batches_.record(commands, pass, world.solid);
            recorded_terrain_draws_ +=
                terrain_.record(commands, world.terrain, sections, translucent_terrain_layers);
            batches_.record(commands, pass, world.blended);
            vkCmdEndRendering(commands);
        });
    }
    if (frame.world && !frame.hand.batches.empty()) {
        add_stage(hand_pass, [this, targets, frame](VkCommandBuffer commands, const GraphImages& images) {
            begin_frame_rendering(commands, attachments(targets, images), std::nullopt);
            batches_.record(commands, {frame.extent, frame.hand.view}, frame.hand.batches);
            vkCmdEndRendering(commands);
        });
    }
    bool is_cleared = frame.world.has_value();
    if (frame.backdrop && targets.scratch) {
        if (!is_cleared || !frame.backdrop->batches.empty()) {
            add_stage(
                backdrop_pass, [this, targets, frame, is_cleared](VkCommandBuffer commands, const GraphImages& images) {
                    const std::optional<std::array<float, 4>> clear_color =
                        is_cleared ? std::nullopt : std::optional(frame.clear_color);
                    begin_frame_rendering(commands, attachments(targets, images), clear_color);
                    batches_.record(commands, {frame.extent, frame.interface.view}, frame.backdrop->batches);
                    vkCmdEndRendering(commands);
                });
            is_cleared = true;
        }
        for (std::uint32_t repeat = 0; repeat < frame.backdrop->blur.repeats; ++repeat) {
            add_blur(targets.color, frame.backdrop->frame, *targets.scratch, BlurAxis::across);
            add_blur(*targets.scratch, frame.backdrop->scratch, targets.color, BlurAxis::down);
        }
    }
    add_stage(interface_pass, [this, targets, frame, is_cleared](VkCommandBuffer commands, const GraphImages& images) {
        const std::optional<std::array<float, 4>> clear_color =
            is_cleared ? std::nullopt : std::optional(frame.clear_color);
        begin_frame_rendering(commands, attachments(targets, images), clear_color);
        batches_.record(commands, {frame.extent, frame.interface.view}, frame.interface.batches);
        vkCmdEndRendering(commands);
    });
}

}
