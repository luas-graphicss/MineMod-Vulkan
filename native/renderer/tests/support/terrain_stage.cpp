#include "support/terrain_stage.hpp"

#include <array>
#include <cmath>
#include <cstddef>
#include <stdexcept>
#include <utility>
#include <vector>

#include "environment/fog.hpp"
#include "frame/frame_rendering.hpp"
#include "graph/graph_images.hpp"
#include "graph/image_use.hpp"
#include "graph/planned_frame.hpp"
#include "graph/render_graph.hpp"
#include "math/matrix.hpp"
#include "terrain/section_buffers.hpp"
#include "terrain/terrain_frame.hpp"
#include "terrain/terrain_layer.hpp"
#include "textures/lightmap.hpp"
#include "textures/mip_chain.hpp"

namespace minemod::renderer::test_support {
namespace {

constexpr VkExtent2D frame_extent{64, 64};
constexpr VkExtent2D atlas_extent{1, 1};
constexpr VkDeviceSize arena_capacity = 4096;
constexpr float frame_reach_at_wall = 8.0f;
constexpr float wall_distance = 40.0f;
constexpr std::array<float, 4> black{0.0f, 0.0f, 0.0f, 1.0f};
constexpr Fog clear_air{black, 1000.0f, 2000.0f};
constexpr std::byte opaque_white_channel{255};

constexpr SamplerDescription atlas_sampler{
    VK_FILTER_NEAREST,
    VK_FILTER_NEAREST,
    VK_SAMPLER_MIPMAP_MODE_NEAREST,
    VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
    1,
};

Matrix4 projection() {
    return perspective({2.0f * std::atan(frame_reach_at_wall / wall_distance), 1.0f, 0.5f, 100.0f});
}

ImportedImage imported(
    const Image& image, VkImageAspectFlags aspect, const ImageUse& entry, const std::optional<ImageUse>& exit) {
    return ImportedImage{image.get(), image.view(), image.extent(), aspect, entry, exit};
}

}

TerrainStage::TerrainStage()
    : targets_(gpu_.device(), frame_extent),
      arena_(gpu_.device(), arena_capacity),
      atlas_(gpu_.device(), atlas_extent, 1),
      lightmap_(gpu_.device(), lightmap_extent, 1),
      samplers_(gpu_.device()),
      pipelines_(gpu_.device()),
      descriptors_(gpu_.device(), DescriptorPoolShape{1, {{VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 2}}, 1}),
      sections_(retirements_),
      renderer_(gpu_.device(), pipelines_) {
    upload_white(atlas_, atlas_extent);
    upload_white(lightmap_, lightmap_extent);
    textures_ = renderer_.bind_textures(
        descriptors_,
        TerrainTextures{
            atlas_.image().view(),
            samplers_.sampler_for(atlas_sampler),
            lightmap_.image().view(),
            samplers_.sampler_for(lightmap_sampler),
        });
}

void TerrainStage::publish(const SectionPosition& section, const SectionMesh& mesh) {
    const JobStamp stamp = epochs_.schedule(section);
    SectionBuffers buffers(gpu_.device(), mesh);
    arena_.reset();
    const StagedSection staged = stage_section(arena_, mesh).value();
    gpu_.device().submit([&](VkCommandBuffer commands) {
        buffers.record_upload(commands, arena_.buffer(), staged);
    });
    if (!sections_.publish(epochs_, {section, stamp}, std::move(buffers), 0)) {
        throw std::logic_error("the stage scheduled a job that was no longer current when it published");
    }
}

FramePixels TerrainStage::render(const TerrainView& view) {
    RenderGraph graph;
    const ImageId color = graph.import_image(
        "frame color", imported(targets_.color(), VK_IMAGE_ASPECT_COLOR_BIT, uses::untouched, uses::transfer_source));
    const ImageId depth = graph.import_image(
        "frame depth", imported(targets_.depth(), VK_IMAGE_ASPECT_DEPTH_BIT, uses::untouched, std::nullopt));
    const ImageId atlas = graph.import_image(
        "block atlas",
        imported(atlas_.image(), VK_IMAGE_ASPECT_COLOR_BIT, uses::sampled_by_fragment_shader, std::nullopt));
    const ImageId lightmap = graph.import_image(
        "lightmap",
        imported(lightmap_.image(), VK_IMAGE_ASPECT_COLOR_BIT, uses::sampled_by_fragment_shader, std::nullopt));

    const TerrainFrame frame{frame_extent, projection(), view.camera, clear_air, textures_, view.reachable_sections};
    graph.add_pass(
        "terrain",
        {
            {color, uses::color_attachment},
            {depth, uses::depth_attachment},
            {atlas, uses::sampled_by_fragment_shader},
            {lightmap, uses::sampled_by_fragment_shader},
        },
        [this, frame, color, depth](VkCommandBuffer commands, const GraphImages& images) {
            begin_frame_rendering(commands, {images.at(color), images.at(depth)}, black);
            draw_count_ = renderer_.record(commands, frame, sections_, terrain_layers);
            vkCmdEndRendering(commands);
        });

    const PlannedFrame planned(gpu_.device(), std::move(graph));
    gpu_.device().submit([&planned](VkCommandBuffer commands) { planned.record(commands); });
    return read_back_color(gpu_.device(), targets_.color());
}

void TerrainStage::upload_white(const Texture& texture, VkExtent2D extent) {
    const std::vector<std::byte> rgba(std::size_t{extent.width} * extent.height * 4, opaque_white_channel);
    const MipChain chain = build_mip_chain(extent, rgba, 1);
    arena_.reset();
    const ArenaRegion staged = arena_.stage(chain.rgba, 4).value();
    gpu_.device().submit([&](VkCommandBuffer commands) {
        texture.record_upload(commands, arena_.buffer(), staged, chain);
    });
}

}
