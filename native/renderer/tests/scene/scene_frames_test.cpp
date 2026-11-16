#include <array>
#include <cstddef>
#include <cstdint>
#include <optional>
#include <string>
#include <utility>
#include <vector>

#include <doctest.h>

#include "descriptors/descriptor_pools.hpp"
#include "draw/batch_geometry.hpp"
#include "draw/batch_renderer.hpp"
#include "draw/material.hpp"
#include "draw/view_constants.hpp"
#include "environment/fog.hpp"
#include "frame/frame_readback.hpp"
#include "frame/frame_targets.hpp"
#include "frame/frames_in_flight.hpp"
#include "graph/image_use.hpp"
#include "graph/planned_frame.hpp"
#include "graph/render_graph.hpp"
#include "lifetime/retirement_queue.hpp"
#include "math/matrix.hpp"
#include "pipelines/pipeline_cache.hpp"
#include "scene/scene_frame.hpp"
#include "scene/scene_renderer.hpp"
#include "support/pixel_colors.hpp"
#include "support/scene_vertices.hpp"
#include "support/validated_device.hpp"
#include "terrain/terrain_renderer.hpp"
#include "terrain/terrain_sections.hpp"
#include "textures/lightmap.hpp"
#include "textures/mip_chain.hpp"
#include "textures/sampler_cache.hpp"
#include "textures/texture.hpp"
#include "upload/upload_arena.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VkExtent2D frame_extent{16, 16};
constexpr VkExtent2D white_extent{1, 1};
constexpr VkExtent2D table_extent{16, 16};
constexpr std::size_t frames_in_flight = 2;
constexpr VkDeviceSize arena_capacity = 1024;
constexpr SpriteRegion darkest_light_texel{0, 0, 1, 1};
constexpr std::array<std::uint8_t, 5> light_levels{255, 204, 153, 102, 51};

constexpr VertexFormat lit_format{VertexElement::position, VertexElement::color, VertexElement::light};
constexpr Material lit_by_table{
    Blending::disabled,
    DepthState{true, true, VK_COMPARE_OP_LESS_OR_EQUAL},
    FaceCulling::back_faces,
    0.0f,
    Shading{.applies_lightmap = true},
};

constexpr SamplerDescription nearest_sampler{
    VK_FILTER_NEAREST,
    VK_FILTER_NEAREST,
    VK_SAMPLER_MIPMAP_MODE_NEAREST,
    VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
    1,
};

constexpr std::array<float, 4> black{0.0f, 0.0f, 0.0f, 1.0f};
constexpr std::array<float, 4> untinted{1.0f, 1.0f, 1.0f, 1.0f};
constexpr SceneView flat_view{
    identity_matrix,
    Fog{black, 1000.0f, 2000.0f},
    {Vector3{0.0f, 1.0f, 0.0f}, Vector3{0.0f, 0.0f, 0.0f}},
};

std::vector<std::byte> solid_texels(VkExtent2D extent, std::uint8_t shade) {
    return std::vector<std::byte>(std::size_t{extent.width} * extent.height * 4, std::byte{shade});
}

struct FrameSlot {
    explicit FrameSlot(const Device& device)
        : uploads(device, arena_capacity), geometry(device, arena_capacity, geometry_arena_usage), view(device) {
        view.write(flat_view);
    }

    UploadArena uploads;
    UploadArena geometry;
    ViewConstants view;
    VkDescriptorSet view_set = VK_NULL_HANDLE;
};

class RunningScene {
public:
    RunningScene()
        : targets_(gpu_.device(), frame_extent),
          white_(gpu_.device(), white_extent, 1),
          lightmap_(gpu_.device(), lightmap_extent, 1),
          overlay_(gpu_.device(), table_extent, 1),
          samplers_(gpu_.device()),
          pipelines_(gpu_.device()),
          descriptors_(
              gpu_.device(),
              DescriptorPoolShape{
                  3,
                  {{VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 2}, {VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 5}},
                  1,
              }),
          sections_(retirements_),
          terrain_(gpu_.device(), pipelines_),
          batches_(gpu_.device(), pipelines_),
          blur_(gpu_.device(), pipelines_, samplers_.sampler_for(nearest_sampler)),
          scene_(terrain_, batches_, blur_),
          slots_{FrameSlot(gpu_.device()), FrameSlot(gpu_.device())},
          frames_(gpu_.device(), frames_in_flight) {
        upload(white_, white_extent);
        upload(lightmap_, lightmap_extent);
        upload(overlay_, table_extent);

        const VkSampler sampler = samplers_.sampler_for(nearest_sampler);
        for (FrameSlot& slot : slots_) {
            slot.view_set = batches_.bind_view(
                descriptors_, slot.view, {lightmap_.image().view(), sampler, overlay_.image().view(), sampler});
        }
        surface_ = batches_.bind_texture(descriptors_, white_.image().view(), sampler);
    }

    Pixel draw_frame_lit_by(std::uint8_t light_level) {
        const FramesInFlight::Frame frame = frames_.begin();
        FrameSlot& slot = slots_[frame.number % slots_.size()];
        slot.uploads.reset();
        slot.geometry.reset();

        const std::array<std::byte, 4> texel{
            std::byte{light_level}, std::byte{light_level}, std::byte{light_level}, std::byte{255}};
        const ArenaRegion staged_light = slot.uploads.stage(texel, 4).value();
        SceneVertex unlit;
        unlit.packed_light = {0, 0};
        const std::vector<std::byte> quad =
            encode_vertices(lit_format, quad_corners({-1.0f, -1.0f, 1.0f, 1.0f, 0.5f}, unlit));
        BatchStager stager(slot.geometry);
        const std::array interface{DrawBatch{
            lit_format,
            stager.stage({lit_format, PrimitiveMode::quads, quad}).value(),
            lit_by_table,
            surface_,
            identity_matrix,
            untinted,
        }};

        RenderGraph graph;
        const ImageId color = graph.import_image("frame color", imported_target(targets_.color()));
        const ImageId depth = graph.import_image(
            "frame depth",
            ImportedImage{
                targets_.depth().get(),
                targets_.depth().view(),
                frame_extent,
                VK_IMAGE_ASPECT_DEPTH_BIT,
                uses::untouched,
                std::nullopt,
            });
        const ImageId lightmap = graph.import_image("lightmap", imported_table(lightmap_.image()));
        graph.add_pass(
            "update lightmap",
            {{lightmap, uses::transfer_destination}},
            [this, &slot, staged_light](VkCommandBuffer commands, const GraphImages&) {
                lightmap_.record_region_update(commands, slot.uploads.buffer(), staged_light, 0, darkest_light_texel);
            });
        scene_.add_passes(
            graph,
            SceneTargets{color, depth, {lightmap}},
            SceneFrame{frame_extent, black, std::nullopt, {slot.view_set, {}}, {slot.view_set, interface}},
            sections_);

        const PlannedFrame planned(gpu_.device(), std::move(graph));
        planned.record(frame.commands);
        frames_.submit();
        return read_back_color(gpu_.device(), targets_.color()).at(frame_extent.width / 2, frame_extent.height / 2);
    }

    std::string validation_text() const {
        return gpu_.validation_text();
    }

private:
    static ImportedImage imported_target(const Image& image) {
        return ImportedImage{
            image.get(), image.view(), image.extent(), VK_IMAGE_ASPECT_COLOR_BIT, uses::untouched, uses::transfer_source};
    }

    static ImportedImage imported_table(const Image& image) {
        return ImportedImage{
            image.get(),
            image.view(),
            image.extent(),
            VK_IMAGE_ASPECT_COLOR_BIT,
            uses::sampled_by_shaders,
            uses::sampled_by_shaders,
        };
    }

    void upload(const Texture& texture, VkExtent2D extent) {
        const MipChain chain = build_mip_chain(extent, solid_texels(extent, 255), 1);
        UploadArena arena(gpu_.device(), arena_capacity);
        const ArenaRegion staged = arena.stage(chain.rgba, 4).value();
        gpu_.device().submit([&](VkCommandBuffer commands) {
            texture.record_upload(commands, arena.buffer(), staged, chain);
        });
    }

    ValidatedDevice gpu_;
    FrameTargets targets_;
    Texture white_;
    Texture lightmap_;
    Texture overlay_;
    SamplerCache samplers_;
    PipelineCache pipelines_;
    DescriptorPools descriptors_;
    RetirementQueue retirements_;
    TerrainSections sections_;
    TerrainRenderer terrain_;
    BatchRenderer batches_;
    FrameBlurRenderer blur_;
    SceneRenderer scene_;
    std::array<FrameSlot, frames_in_flight> slots_;
    FramesInFlight frames_;
    VkDescriptorSet surface_ = VK_NULL_HANDLE;
};

}

TEST_CASE("every frame sees the lightmap texel written at its start while earlier frames reuse their slots") {
    RunningScene scene;

    for (const std::uint8_t light_level : light_levels) {
        const Pixel drawn = scene.draw_frame_lit_by(light_level);

        CHECK_MESSAGE(is_close(drawn, {light_level, light_level, light_level}), describe(drawn));
    }
    CHECK(scene.validation_text() == "");
}
