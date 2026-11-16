#include <array>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <cstdlib>
#include <optional>
#include <span>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

#include <doctest.h>

#include "descriptors/descriptor_pools.hpp"
#include "environment/fog.hpp"
#include "frame/frame_readback.hpp"
#include "frame/frame_targets.hpp"
#include "graph/graph_images.hpp"
#include "graph/image_use.hpp"
#include "graph/planned_frame.hpp"
#include "graph/render_graph.hpp"
#include "lifetime/retirement_queue.hpp"
#include "math/matrix.hpp"
#include "pipelines/pipeline_cache.hpp"
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
constexpr VkExtent2D atlas_extent{2, 1};
constexpr VkDeviceSize arena_capacity = 4096;
constexpr WorldPosition camera{8.0, 8.0, 0.0};
constexpr float wall_distance = 40.0f;
constexpr float wall_reach = 8.0f;
constexpr int channel_tolerance = 2;

constexpr SectionPosition wall_section{0, 0, -3};
constexpr SectionPosition middle_section{0, 0, -2};
constexpr SectionPosition near_section{0, 0, -1};
constexpr SectionPosition section_behind_camera{0, 0, 1};

constexpr SamplerDescription atlas_sampler{
    VK_FILTER_NEAREST,
    VK_FILTER_NEAREST,
    VK_SAMPLER_MIPMAP_MODE_NEAREST,
    VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
    1,
};

constexpr std::array<float, 2> opaque_texel{0.25f, 0.5f};
constexpr std::array<float, 2> transparent_texel{0.75f, 0.5f};
constexpr std::array<std::uint16_t, 2> full_light{brightest_packed_light, brightest_packed_light};
constexpr std::array<std::uint16_t, 2> no_light{0, 0};
constexpr std::uint8_t unlit_level = 128;

constexpr std::array<std::uint8_t, 4> red{255, 0, 0, 255};
constexpr std::array<std::uint8_t, 4> green{0, 255, 0, 255};
constexpr std::array<std::uint8_t, 4> half_yellow{255, 255, 0, 128};
constexpr std::array<std::uint8_t, 4> half_blue{0, 0, 255, 128};

constexpr Fog clear_air{{0.0f, 0.0f, 0.0f, 1.0f}, 1000.0f, 2000.0f};
constexpr std::array<float, 4> blue_haze{0.0f, 0.0f, 1.0f, 1.0f};

struct Rgb {
    int red;
    int green;
    int blue;
};

constexpr VkOffset2D upper_left_outer{8, 16};
constexpr VkOffset2D upper_left_inner{24, 16};
constexpr VkOffset2D upper_right{48, 16};
constexpr VkOffset2D lower_left_outer{8, 48};
constexpr VkOffset2D lower_left_inner{24, 48};
constexpr VkOffset2D lower_right{48, 48};

Matrix4 scene_projection() {
    return perspective({2.0f * std::atan(wall_reach / wall_distance), 1.0f, 0.5f, 100.0f});
}

std::vector<std::byte> texels(std::span<const std::uint8_t> channels) {
    const std::span<const std::byte> bytes = std::as_bytes(channels);
    return {bytes.begin(), bytes.end()};
}

std::vector<std::byte> atlas_texels() {
    constexpr std::array<std::uint8_t, 8> opaque_then_transparent{255, 255, 255, 255, 255, 255, 255, 0};
    return texels(opaque_then_transparent);
}

std::vector<std::byte> lightmap_texels() {
    std::vector<std::uint8_t> channels(std::size_t{lightmap_extent.width} * lightmap_extent.height * 4, 255);
    channels[0] = unlit_level;
    channels[1] = unlit_level;
    channels[2] = unlit_level;
    return texels(channels);
}

SectionMesh wall_mesh(const std::array<std::uint16_t, 2>& packed_light) {
    SectionMesh mesh{};
    append_quad(mesh, TerrainLayer::solid, {0.0f, 0.0f, 16.0f, 16.0f, 8.0f}, {red, opaque_texel, packed_light});
    return mesh;
}

SectionMesh middle_mesh() {
    SectionMesh mesh{};
    append_quad(mesh, TerrainLayer::cutout_mipped, {5.6f, 3.2f, 8.0f, 12.8f, 8.0f}, {green, opaque_texel, full_light});
    append_quad(mesh, TerrainLayer::cutout, {3.2f, 3.2f, 5.6f, 12.8f, 8.0f}, {green, transparent_texel, full_light});
    append_quad(
        mesh, TerrainLayer::translucent, {8.0f, 2.4f, 13.6f, 8.0f, 4.0f}, {half_yellow, opaque_texel, full_light});
    return mesh;
}

SectionMesh near_mesh() {
    SectionMesh mesh{};
    append_quad(mesh, TerrainLayer::translucent, {6.4f, 6.4f, 9.6f, 8.0f, 8.0f}, {half_blue, opaque_texel, full_light});
    return mesh;
}

ImportedImage imported(
    const Image& image, VkImageAspectFlags aspect, const ImageUse& entry, const std::optional<ImageUse>& exit) {
    return ImportedImage{image.get(), image.view(), image.extent(), aspect, entry, exit};
}

void begin_cleared_rendering(VkCommandBuffer commands, const BoundImage& color_target, const BoundImage& depth_target) {
    const VkRenderingAttachmentInfo color{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = color_target.view,
        .imageLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
        .loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR,
        .storeOp = VK_ATTACHMENT_STORE_OP_STORE,
        .clearValue = {.color = {.float32 = {0.0f, 0.0f, 0.0f, 1.0f}}},
    };
    const VkRenderingAttachmentInfo depth{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = depth_target.view,
        .imageLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
        .loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR,
        .storeOp = VK_ATTACHMENT_STORE_OP_DONT_CARE,
        .clearValue = {.depthStencil = {1.0f, 0}},
    };
    const VkRenderingInfo rendering{
        .sType = VK_STRUCTURE_TYPE_RENDERING_INFO,
        .renderArea = {{0, 0}, color_target.extent},
        .layerCount = 1,
        .colorAttachmentCount = 1,
        .pColorAttachments = &color,
        .pDepthAttachment = &depth,
    };
    vkCmdBeginRendering(commands, &rendering);
}

float wall_corner_distance() {
    return std::hypot(wall_reach, wall_reach, wall_distance);
}

std::string describe(const Pixel& pixel) {
    return "drawn " + std::to_string(pixel.red) + ", " + std::to_string(pixel.green) + ", "
        + std::to_string(pixel.blue);
}

bool is_close(int drawn, int expected) {
    return std::abs(drawn - expected) <= channel_tolerance;
}

class TerrainScene {
public:
    TerrainScene()
        : targets_(gpu_.device(), frame_extent),
          arena_(gpu_.device(), arena_capacity),
          atlas_(gpu_.device(), atlas_extent, 1),
          lightmap_(gpu_.device(), lightmap_extent, 1),
          samplers_(gpu_.device()),
          pipelines_(gpu_.device()),
          descriptors_(gpu_.device(), DescriptorPoolShape{1, {{VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 2}}, 1}),
          sections_(retirements_),
          renderer_(gpu_.device(), pipelines_) {
        upload(atlas_, atlas_extent, atlas_texels());
        upload(lightmap_, lightmap_extent, lightmap_texels());
        textures_ = renderer_.bind_textures(
            descriptors_,
            TerrainTextures{
                atlas_.image().view(),
                samplers_.sampler_for(atlas_sampler),
                lightmap_.image().view(),
                samplers_.sampler_for(lightmap_sampler),
            });
    }

    void add_section(const SectionPosition& section, const SectionMesh& mesh) {
        const JobStamp stamp = epochs_.schedule(section);
        SectionBuffers buffers(gpu_.device(), mesh);
        arena_.reset();
        const StagedSection staged = stage_section(arena_, mesh).value();
        gpu_.device().submit([&](VkCommandBuffer commands) {
            buffers.record_upload(commands, arena_.buffer(), staged);
        });
        REQUIRE(sections_.publish(epochs_, {section, stamp}, std::move(buffers), 0));
    }

    FramePixels render(const Fog& fog) {
        RenderGraph graph;
        const ImageId color = graph.import_image(
            "frame color",
            imported(targets_.color(), VK_IMAGE_ASPECT_COLOR_BIT, uses::untouched, uses::transfer_source));
        const ImageId depth = graph.import_image(
            "frame depth", imported(targets_.depth(), VK_IMAGE_ASPECT_DEPTH_BIT, uses::untouched, std::nullopt));
        const ImageId atlas = graph.import_image(
            "block atlas",
            imported(atlas_.image(), VK_IMAGE_ASPECT_COLOR_BIT, uses::sampled_by_fragment_shader, std::nullopt));
        const ImageId lightmap = graph.import_image(
            "lightmap",
            imported(lightmap_.image(), VK_IMAGE_ASPECT_COLOR_BIT, uses::sampled_by_fragment_shader, std::nullopt));

        const TerrainFrame frame{frame_extent, scene_projection(), camera, fog, textures_};
        graph.add_pass(
            "terrain",
            {
                {color, uses::color_attachment},
                {depth, uses::depth_attachment},
                {atlas, uses::sampled_by_fragment_shader},
                {lightmap, uses::sampled_by_fragment_shader},
            },
            [this, frame, color, depth](VkCommandBuffer commands, const GraphImages& images) {
                begin_cleared_rendering(commands, images.at(color), images.at(depth));
                draw_count_ = renderer_.record(commands, frame, sections_, terrain_layers);
                vkCmdEndRendering(commands);
            });

        const PlannedFrame planned(gpu_.device(), std::move(graph));
        gpu_.device().submit([&planned](VkCommandBuffer commands) { planned.record(commands); });
        return read_back_color(gpu_.device(), targets_.color());
    }

    std::uint32_t draw_count() const noexcept {
        return draw_count_;
    }

    std::string validation_text() const {
        return gpu_.validation_text();
    }

private:
    void upload(const Texture& texture, VkExtent2D extent, const std::vector<std::byte>& rgba) {
        const MipChain chain = build_mip_chain(extent, rgba, 1);
        arena_.reset();
        const ArenaRegion staged = arena_.stage(chain.rgba, 4).value();
        gpu_.device().submit([&](VkCommandBuffer commands) {
            texture.record_upload(commands, arena_.buffer(), staged, chain);
        });
    }

    ValidatedDevice gpu_;
    FrameTargets targets_;
    UploadArena arena_;
    Texture atlas_;
    Texture lightmap_;
    SamplerCache samplers_;
    PipelineCache pipelines_;
    DescriptorPools descriptors_;
    RetirementQueue retirements_;
    JobEpochs epochs_;
    TerrainSections sections_;
    TerrainRenderer renderer_;
    VkDescriptorSet textures_ = VK_NULL_HANDLE;
    std::uint32_t draw_count_ = 0;
};

void check_pixel(const FramePixels& pixels, const VkOffset2D& position, const Rgb& expected) {
    const Pixel drawn = pixels.at(static_cast<std::uint32_t>(position.x), static_cast<std::uint32_t>(position.y));
    const bool matches = is_close(drawn.red, expected.red) && is_close(drawn.green, expected.green)
        && is_close(drawn.blue, expected.blue);
    CHECK_MESSAGE(matches, describe(drawn));
}

}

TEST_CASE("opaque, cutout and translucent terrain compose in depth order") {
    TerrainScene scene;
    scene.add_section(wall_section, wall_mesh(full_light));
    scene.add_section(middle_section, middle_mesh());
    scene.add_section(near_section, near_mesh());

    const FramePixels pixels = scene.render(clear_air);

    check_pixel(pixels, upper_left_outer, {255, 0, 0});
    check_pixel(pixels, upper_left_inner, {0, 255, 0});
    check_pixel(pixels, upper_right, {255, 0, 0});
    check_pixel(pixels, lower_left_outer, {127, 0, 128});
    check_pixel(pixels, lower_left_inner, {0, 127, 128});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("translucent sections blend from the farthest to the nearest") {
    TerrainScene scene;
    scene.add_section(near_section, near_mesh());
    scene.add_section(middle_section, middle_mesh());
    scene.add_section(wall_section, wall_mesh(full_light));

    const FramePixels pixels = scene.render(clear_air);

    check_pixel(pixels, lower_right, {127, 64, 128});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("sections outside the view are not drawn") {
    TerrainScene scene;
    scene.add_section(wall_section, wall_mesh(full_light));
    scene.add_section(middle_section, middle_mesh());
    scene.add_section(near_section, near_mesh());
    scene.render(clear_air);
    const std::uint32_t draws_in_view = scene.draw_count();

    scene.add_section(section_behind_camera, wall_mesh(full_light));
    scene.render(clear_air);

    CHECK(draws_in_view == 5);
    CHECK(scene.draw_count() == draws_in_view);
}

TEST_CASE("fog fades terrain toward its color with the distance of its vertices from the camera") {
    TerrainScene scene;
    scene.add_section(wall_section, wall_mesh(full_light));
    const Fog partial{blue_haze, 0.0f, 2.0f * wall_distance};
    const Fog complete{blue_haze, 0.0f, wall_distance / 2.0f};
    const float haze = fog_factor(partial, wall_corner_distance());
    const int hazed_red = static_cast<int>(std::lround(255.0f * (1.0f - haze)));
    const int hazed_blue = static_cast<int>(std::lround(255.0f * haze));

    check_pixel(scene.render(partial), upper_right, {hazed_red, 0, hazed_blue});
    check_pixel(scene.render(complete), upper_right, {0, 0, 255});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("the lightmap darkens terrain by its packed light") {
    TerrainScene scene;
    scene.add_section(wall_section, wall_mesh(no_light));

    check_pixel(scene.render(clear_air), upper_right, {unlit_level, 0, 0});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("fog without depth is rejected before anything is recorded") {
    const ValidatedDevice gpu;
    PipelineCache pipelines(gpu.device());
    RetirementQueue retirements;
    const TerrainSections sections(retirements);
    const TerrainRenderer renderer(gpu.device(), pipelines);
    const TerrainFrame frame{frame_extent, scene_projection(), camera, Fog{blue_haze, 10.0f, 10.0f}, VK_NULL_HANDLE};

    CHECK_THROWS_AS(renderer.record(VK_NULL_HANDLE, frame, sections, terrain_layers), std::invalid_argument);
}
