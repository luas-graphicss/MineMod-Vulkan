#include "scene/scene_renderer.hpp"

#include <array>
#include <functional>
#include <optional>
#include <span>
#include <string>
#include <utility>

#include "frame/frame_rendering.hpp"
#include "graph/graph_images.hpp"
#include "graph/image_use.hpp"
#include "terrain/terrain_layer.hpp"

namespace minemod::renderer {

namespace {

using DrawStage = std::function<void(VkCommandBuffer)>;

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

std::optional<SceneStage> SceneRenderer::add_passes(
    RenderGraph& graph, const SceneTargets& targets, const SceneFrame& frame, const TerrainSections& sections) {
    const std::vector<ImageAccess> accesses = stage_accesses(targets);
    const VkAttachmentStoreOp depth_store =
        frame.part.is_handed_over ? VK_ATTACHMENT_STORE_OP_STORE : VK_ATTACHMENT_STORE_OP_DONT_CARE;
    std::optional<PassId> previous;
    std::optional<SceneStage> drawn_stage;
    bool is_cleared = frame.part.resumed_stage.has_value();
    const auto add_pass = [&](std::string_view name, std::vector<ImageAccess> used, RecordPass record) {
        const PassId pass = graph.add_pass(std::string(name), std::move(used), std::move(record));
        if (previous) {
            graph.add_dependency(pass, *previous);
        }
        previous = pass;
    };
    const auto add_stage = [&](SceneStage stage, std::string_view name, DrawStage draw) {
        const bool resumes_depth = !drawn_stage && frame.part.resumed_stage == stage;
        const std::optional<std::array<float, 4>> clear_color =
            is_cleared ? std::nullopt : std::optional(frame.clear_color);
        const DepthOperations depth{
            resumes_depth ? VK_ATTACHMENT_LOAD_OP_LOAD : VK_ATTACHMENT_LOAD_OP_CLEAR,
            depth_store,
        };
        add_pass(
            name,
            accesses,
            [targets, clear_color, depth, draw = std::move(draw)](VkCommandBuffer commands, const GraphImages& images) {
                begin_frame_rendering(commands, attachments(targets, images), clear_color, depth);
                draw(commands);
                vkCmdEndRendering(commands);
            });
        is_cleared = true;
        drawn_stage = stage;
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
        add_stage(SceneStage::world, world_pass, [this, frame, &sections](VkCommandBuffer commands) {
            const WorldStage& world = *frame.world;
            const BatchPass pass{frame.extent, world.view};
            const auto record_terrain = [&](std::span<const TerrainLayer> layers) {
                return world.draws_terrain ? terrain_.record(commands, world.terrain, sections, layers) : 0u;
            };
            batches_.record(commands, pass, world.sky);
            recorded_terrain_draws_ = record_terrain(opaque_terrain_layers);
            batches_.record(commands, pass, world.solid);
            recorded_terrain_draws_ += record_terrain(translucent_terrain_layers);
            batches_.record(commands, pass, world.blended);
        });
    }
    if (!frame.hand.batches.empty()) {
        add_stage(SceneStage::hand, hand_pass, [this, frame](VkCommandBuffer commands) {
            batches_.record(commands, {frame.extent, frame.hand.view}, frame.hand.batches);
        });
    }
    if (frame.backdrop && targets.scratch) {
        if (!is_cleared || !frame.backdrop->batches.empty()) {
            add_stage(SceneStage::interface, backdrop_pass, [this, frame](VkCommandBuffer commands) {
                batches_.record(commands, {frame.extent, frame.interface.view}, frame.backdrop->batches);
            });
        }
        for (std::uint32_t repeat = 0; repeat < frame.backdrop->blur.repeats; ++repeat) {
            add_blur(targets.color, frame.backdrop->frame, *targets.scratch, BlurAxis::across);
            add_blur(*targets.scratch, frame.backdrop->scratch, targets.color, BlurAxis::down);
        }
    }
    if (!frame.part.is_handed_over || !is_cleared || !frame.interface.batches.empty()) {
        add_stage(SceneStage::interface, interface_pass, [this, frame](VkCommandBuffer commands) {
            batches_.record(commands, {frame.extent, frame.interface.view}, frame.interface.batches);
        });
    }
    return drawn_stage;
}

}
