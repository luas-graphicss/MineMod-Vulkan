#include <array>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <optional>
#include <span>
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
#include "support/terrain_quads.hpp"
#include "support/validated_device.hpp"
#include "terrain/section_buffers.hpp"
#include "terrain/terrain_renderer.hpp"
#include "terrain/terrain_sections.hpp"
#include "textures/lightmap.hpp"
#include "textures/mip_chain.hpp"
#include "textures/sampler_cache.hpp"
#include "textures/texture.hpp"
#include "upload/upload_arena.hpp"
#include "work/job_epochs.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VkExtent2D frame_extent{64, 64};
constexpr VkExtent2D white_extent{1, 1};
constexpr VkExtent2D table_extent{16, 16};
constexpr VkDeviceSize upload_capacity = 4096;
constexpr VkDeviceSize geometry_capacity = 8192;
constexpr WorldPosition camera{8.0, 8.0, 0.0};
constexpr float wall_distance = 40.0f;
constexpr float wall_reach = 8.0f;

constexpr SectionPosition wall_section{0, 0, -3};
constexpr SectionPosition water_section{0, 0, -2};

constexpr VertexFormat colored_format{VertexElement::position, VertexElement::color};

constexpr SamplerDescription nearest_sampler{
    VK_FILTER_NEAREST,
    VK_FILTER_NEAREST,
    VK_SAMPLER_MIPMAP_MODE_NEAREST,
    VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
    1,
};

constexpr DepthState depth_tested{true, true, VK_COMPARE_OP_LESS_OR_EQUAL};
constexpr DepthState depth_read_only{true, false, VK_COMPARE_OP_LESS_OR_EQUAL};
constexpr DepthState depth_ignored{false, false, VK_COMPARE_OP_ALWAYS};
constexpr Material opaque{Blending::disabled, depth_tested, FaceCulling::back_faces, 0.0f, {}};
constexpr Material translucent{Blending::alpha, depth_read_only, FaceCulling::back_faces, 0.0f, {}};
constexpr Material backdrop{Blending::disabled, depth_ignored, FaceCulling::back_faces, 0.0f, {}};

constexpr std::array<float, 4> untinted{1.0f, 1.0f, 1.0f, 1.0f};
constexpr std::array<float, 4> dark_gray{32.0f / 255.0f, 32.0f / 255.0f, 32.0f / 255.0f, 1.0f};
constexpr Fog clear_air{{0.0f, 0.0f, 0.0f, 1.0f}, 1000.0f, 2000.0f};
constexpr std::array<Vector3, 2> lights{Vector3{0.0f, 1.0f, 0.0f}, Vector3{0.0f, 0.0f, 0.0f}};

constexpr std::array<float, 2> white_texel{0.5f, 0.5f};
constexpr std::array<std::uint16_t, 2> full_light{brightest_packed_light, brightest_packed_light};

constexpr std::array<std::uint8_t, 4> red{255, 0, 0, 255};
constexpr std::array<std::uint8_t, 4> green{0, 255, 0, 255};
constexpr std::array<std::uint8_t, 4> cyan{0, 255, 255, 255};
constexpr std::array<std::uint8_t, 4> orange{255, 128, 0, 255};
constexpr std::array<std::uint8_t, 4> magenta{255, 0, 255, 255};
constexpr std::array<std::uint8_t, 4> white{255, 255, 255, 255};
constexpr std::array<std::uint8_t, 4> half_blue{0, 0, 255, 128};
constexpr std::array<std::uint8_t, 4> half_green{0, 255, 0, 128};

constexpr QuadPlacement sky_dome{-18.0f, -18.0f, 18.0f, 18.0f, -90.0f};
constexpr QuadPlacement wall_right_of_first_strip{4.0f, 0.0f, 16.0f, 16.0f, 8.0f};
constexpr QuadPlacement water_over_right_half{8.0f, 3.2f, 12.8f, 12.8f, 8.0f};
constexpr QuadPlacement entity_in_third_strip{0.0f, -6.0f, 3.0f, 6.0f, -30.0f};
constexpr QuadPlacement particle_in_fourth_strip{1.6f, -3.2f, 3.2f, 3.2f, -16.0f};
constexpr QuadPlacement hand_in_lower_right_corner{0.5f, -1.0f, 1.0f, -0.5f, 0.999f};

constexpr float interface_depth = 0.9995f;
constexpr VkRect2D badge_in_upper_left_corner{{0, 0}, {16, 16}};
constexpr VkRect2D badge_over_hand{{56, 56}, {8, 8}};

constexpr VkOffset2D sky_strip{8, 24};
constexpr VkOffset2D wall_strip{24, 24};
constexpr VkOffset2D entity_strip{40, 24};
constexpr VkOffset2D particle_strip{56, 24};
constexpr VkOffset2D hand_corner{52, 52};
constexpr VkOffset2D upper_left_badge{8, 8};
constexpr VkOffset2D badge_on_hand{60, 60};
constexpr VkOffset2D frame_center{32, 40};

Matrix4 world_projection() {
    return perspective({2.0f * std::atan(wall_reach / wall_distance), 1.0f, 0.5f, 100.0f});
}

Matrix4 pixel_projection(VkExtent2D extent) {
    Matrix4 projection = identity_matrix;
    projection.columns[0] = 2.0f / static_cast<float>(extent.width);
    projection.columns[5] = -2.0f / static_cast<float>(extent.height);
    projection.columns[12] = -1.0f;
    projection.columns[13] = 1.0f;
    return projection;
}

std::vector<std::byte> solid_texels(VkExtent2D extent, const std::array<std::uint8_t, 4>& color) {
    std::vector<std::byte> bytes;
    for (std::uint32_t texel = 0; texel < extent.width * extent.height; ++texel) {
        for (const std::uint8_t channel : color) {
            bytes.push_back(std::byte{channel});
        }
    }
    return bytes;
}

SceneVertex colored(const std::array<std::uint8_t, 4>& color) {
    SceneVertex vertex;
    vertex.color = color;
    return vertex;
}

std::array<SceneVertex, 4> interface_quad(const VkRect2D& pixels, const std::array<std::uint8_t, 4>& color) {
    const auto left = static_cast<float>(pixels.offset.x);
    const auto top = static_cast<float>(pixels.offset.y);
    const float right = left + static_cast<float>(pixels.extent.width);
    const float bottom = top + static_cast<float>(pixels.extent.height);

    std::array<SceneVertex, 4> corners{colored(color), colored(color), colored(color), colored(color)};
    corners[0].position = {left, top, interface_depth};
    corners[1].position = {left, bottom, interface_depth};
    corners[2].position = {right, bottom, interface_depth};
    corners[3].position = {right, top, interface_depth};
    return corners;
}

SectionMesh terrain_quad(TerrainLayer layer, const QuadPlacement& placement, const std::array<std::uint8_t, 4>& color) {
    SectionMesh mesh{};
    append_quad(mesh, layer, placement, {color, white_texel, full_light});
    return mesh;
}

ImportedImage imported(
    const Image& image, VkImageAspectFlags aspect, const ImageUse& entry, const std::optional<ImageUse>& exit) {
    return ImportedImage{image.get(), image.view(), image.extent(), aspect, entry, exit};
}

class ComposedScene {
public:
    ComposedScene()
        : targets_(gpu_.device(), frame_extent),
          uploads_(gpu_.device(), upload_capacity),
          geometry_(gpu_.device(), geometry_capacity, geometry_arena_usage),
          stager_(geometry_),
          white_(gpu_.device(), white_extent, 1),
          lightmap_(gpu_.device(), lightmap_extent, 1),
          overlay_(gpu_.device(), table_extent, 1),
          samplers_(gpu_.device()),
          pipelines_(gpu_.device()),
          descriptors_(
              gpu_.device(),
              DescriptorPoolShape{
                  5,
                  {{VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 3}, {VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 9}},
                  1,
              }),
          sections_(retirements_),
          terrain_(gpu_.device(), pipelines_),
          batches_(gpu_.device(), pipelines_),
          scene_(terrain_, batches_),
          world_constants_(gpu_.device()),
          hand_constants_(gpu_.device()),
          interface_constants_(gpu_.device()) {
        upload(white_, white_extent, white);
        upload(lightmap_, lightmap_extent, white);
        upload(overlay_, table_extent, {0, 0, 0, 255});

        const VkSampler sampler = samplers_.sampler_for(nearest_sampler);
        const ViewTextures tables{lightmap_.image().view(), sampler, overlay_.image().view(), sampler};
        world_constants_.write({world_projection(), clear_air, lights});
        hand_constants_.write({identity_matrix, clear_air, lights});
        interface_constants_.write({pixel_projection(frame_extent), clear_air, lights});
        world_view_ = batches_.bind_view(descriptors_, world_constants_, tables);
        hand_view_ = batches_.bind_view(descriptors_, hand_constants_, tables);
        interface_view_ = batches_.bind_view(descriptors_, interface_constants_, tables);
        surface_ = batches_.bind_texture(descriptors_, white_.image().view(), sampler);
        terrain_textures_ = terrain_.bind_textures(
            descriptors_, TerrainTextures{white_.image().view(), sampler, lightmap_.image().view(), sampler});
    }

    void add_section(const SectionPosition& section, const SectionMesh& mesh) {
        const JobStamp stamp = epochs_.schedule(section);
        SectionBuffers buffers(gpu_.device(), mesh);
        uploads_.reset();
        const StagedSection staged = stage_section(uploads_, mesh).value();
        gpu_.device().submit([&](VkCommandBuffer commands) {
            buffers.record_upload(commands, uploads_.buffer(), staged);
        });
        REQUIRE(sections_.publish(epochs_, {section, stamp}, std::move(buffers), 0));
    }

    DrawBatch batch(std::span<const SceneVertex> quad, const Material& material) {
        const std::vector<std::byte> bytes = encode_vertices(colored_format, quad);
        const BatchGeometry staged = stager_.stage({colored_format, PrimitiveMode::quads, bytes}).value();
        return DrawBatch{colored_format, staged, material, surface_, identity_matrix, untinted};
    }

    WorldStage world(
        std::span<const DrawBatch> sky, std::span<const DrawBatch> solid, std::span<const DrawBatch> blended) const {
        const TerrainFrame terrain{frame_extent, world_projection(), camera, clear_air, terrain_textures_};
        return WorldStage{world_view_, terrain, sky, solid, blended};
    }

    FramePixels render(
        const std::optional<WorldStage>& world_stage,
        std::span<const DrawBatch> hand,
        std::span<const DrawBatch> interface) {
        RenderGraph graph;
        const ImageId color = graph.import_image(
            "frame color",
            imported(targets_.color(), VK_IMAGE_ASPECT_COLOR_BIT, uses::untouched, uses::transfer_source));
        const ImageId depth = graph.import_image(
            "frame depth", imported(targets_.depth(), VK_IMAGE_ASPECT_DEPTH_BIT, uses::untouched, std::nullopt));
        const ImageId lightmap = graph.import_image(
            "lightmap",
            imported(lightmap_.image(), VK_IMAGE_ASPECT_COLOR_BIT, uses::sampled_by_shaders, std::nullopt));

        const SceneFrame frame{frame_extent, dark_gray, world_stage, {hand_view_, hand}, {interface_view_, interface}};
        scene_.add_passes(graph, SceneTargets{color, depth, {lightmap}}, frame, sections_);
        pass_count_ = graph.passes().size();

        const PlannedFrame planned(gpu_.device(), std::move(graph));
        gpu_.device().submit([&planned](VkCommandBuffer commands) { planned.record(commands); });
        return read_back_color(gpu_.device(), targets_.color());
    }

    std::size_t pass_count() const noexcept {
        return pass_count_;
    }

    std::string validation_text() const {
        return gpu_.validation_text();
    }

private:
    void upload(const Texture& texture, VkExtent2D extent, const std::array<std::uint8_t, 4>& color) {
        const MipChain chain = build_mip_chain(extent, solid_texels(extent, color), 1);
        uploads_.reset();
        const ArenaRegion staged = uploads_.stage(chain.rgba, 4).value();
        gpu_.device().submit([&](VkCommandBuffer commands) {
            texture.record_upload(commands, uploads_.buffer(), staged, chain);
        });
    }

    ValidatedDevice gpu_;
    FrameTargets targets_;
    UploadArena uploads_;
    UploadArena geometry_;
    BatchStager stager_;
    Texture white_;
    Texture lightmap_;
    Texture overlay_;
    SamplerCache samplers_;
    PipelineCache pipelines_;
    DescriptorPools descriptors_;
    RetirementQueue retirements_;
    JobEpochs epochs_;
    TerrainSections sections_;
    TerrainRenderer terrain_;
    BatchRenderer batches_;
    SceneRenderer scene_;
    ViewConstants world_constants_;
    ViewConstants hand_constants_;
    ViewConstants interface_constants_;
    VkDescriptorSet world_view_ = VK_NULL_HANDLE;
    VkDescriptorSet hand_view_ = VK_NULL_HANDLE;
    VkDescriptorSet interface_view_ = VK_NULL_HANDLE;
    VkDescriptorSet surface_ = VK_NULL_HANDLE;
    VkDescriptorSet terrain_textures_ = VK_NULL_HANDLE;
    std::size_t pass_count_ = 0;
};

void check_pixel(const FramePixels& pixels, const VkOffset2D& position, const Rgb& expected) {
    const Pixel drawn = pixels.at(static_cast<std::uint32_t>(position.x), static_cast<std::uint32_t>(position.y));
    CHECK_MESSAGE(is_close(drawn, expected), describe(drawn));
}

}

TEST_CASE("a world frame draws sky, terrain and batches in the order that keeps translucency correct") {
    ComposedScene scene;
    scene.add_section(wall_section, terrain_quad(TerrainLayer::solid, wall_right_of_first_strip, red));
    scene.add_section(water_section, terrain_quad(TerrainLayer::translucent, water_over_right_half, half_blue));
    const std::array sky{scene.batch(quad_corners(sky_dome, colored(cyan)), backdrop)};
    const std::array solid{scene.batch(quad_corners(entity_in_third_strip, colored(green)), opaque)};
    const std::array blended{scene.batch(quad_corners(particle_in_fourth_strip, colored(half_green)), translucent)};

    const FramePixels pixels = scene.render(scene.world(sky, solid, blended), {}, {});

    check_pixel(pixels, sky_strip, {0, 255, 255});
    check_pixel(pixels, wall_strip, {255, 0, 0});
    check_pixel(pixels, entity_strip, {0, 127, 128});
    check_pixel(pixels, particle_strip, {63, 128, 64});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("the hand and the interface are drawn over the world whatever depth the world left behind") {
    ComposedScene scene;
    scene.add_section(wall_section, terrain_quad(TerrainLayer::solid, wall_right_of_first_strip, red));
    const std::array hand{scene.batch(quad_corners(hand_in_lower_right_corner, colored(orange)), opaque)};
    const std::array interface{
        scene.batch(interface_quad(badge_in_upper_left_corner, white), opaque),
        scene.batch(interface_quad(badge_over_hand, magenta), opaque),
    };

    const FramePixels pixels = scene.render(scene.world({}, {}, {}), hand, interface);

    check_pixel(pixels, wall_strip, {255, 0, 0});
    check_pixel(pixels, hand_corner, {255, 128, 0});
    check_pixel(pixels, upper_left_badge, {255, 255, 255});
    check_pixel(pixels, badge_on_hand, {255, 0, 255});
    CHECK(scene.pass_count() == 3);
    CHECK(scene.validation_text() == "");
}

TEST_CASE("an interface drawn before any world exists clears the frame itself") {
    ComposedScene scene;
    const std::array interface{scene.batch(interface_quad(badge_in_upper_left_corner, white), opaque)};

    const FramePixels pixels = scene.render(std::nullopt, {}, interface);

    check_pixel(pixels, upper_left_badge, {255, 255, 255});
    check_pixel(pixels, frame_center, {32, 32, 32});
    CHECK(scene.pass_count() == 1);
    CHECK(scene.validation_text() == "");
}

TEST_CASE("a world without anything in the hand skips the hand stage") {
    ComposedScene scene;
    scene.add_section(wall_section, terrain_quad(TerrainLayer::solid, wall_right_of_first_strip, red));

    const FramePixels pixels = scene.render(scene.world({}, {}, {}), {}, {});

    check_pixel(pixels, wall_strip, {255, 0, 0});
    check_pixel(pixels, sky_strip, {32, 32, 32});
    CHECK(scene.pass_count() == 2);
    CHECK(scene.validation_text() == "");
}
