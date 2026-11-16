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
#include "frame/frame_rendering.hpp"
#include "frame/frame_targets.hpp"
#include "graph/graph_images.hpp"
#include "graph/image_use.hpp"
#include "graph/planned_frame.hpp"
#include "graph/render_graph.hpp"
#include "math/matrix.hpp"
#include "pipelines/pipeline_cache.hpp"
#include "support/pixel_colors.hpp"
#include "support/scene_vertices.hpp"
#include "support/validated_device.hpp"
#include "textures/lightmap.hpp"
#include "textures/mip_chain.hpp"
#include "textures/sampler_cache.hpp"
#include "textures/texture.hpp"
#include "upload/upload_arena.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VkExtent2D frame_extent{64, 64};
constexpr VkExtent2D surface_extent{2, 1};
constexpr VkExtent2D overlay_extent{16, 16};
constexpr VkDeviceSize upload_capacity = 4096;
constexpr VkDeviceSize geometry_capacity = 8192;

constexpr VertexFormat colored_format{VertexElement::position, VertexElement::color};
constexpr VertexFormat textured_format{VertexElement::position, VertexElement::color, VertexElement::texture};
constexpr VertexFormat entity_format{
    VertexElement::position,
    VertexElement::color,
    VertexElement::texture,
    VertexElement::overlay,
    VertexElement::light,
    VertexElement::normal,
};

constexpr SamplerDescription nearest_sampler{
    VK_FILTER_NEAREST,
    VK_FILTER_NEAREST,
    VK_SAMPLER_MIPMAP_MODE_NEAREST,
    VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
    1,
};

constexpr DepthState depth_tested{true, true, VK_COMPARE_OP_LESS_OR_EQUAL};
constexpr DepthState depth_read_only{true, false, VK_COMPARE_OP_LESS_OR_EQUAL};
constexpr Material opaque{Blending::disabled, depth_tested, FaceCulling::back_faces, 0.0f, {}};
constexpr Material translucent{Blending::alpha, depth_read_only, FaceCulling::back_faces, 0.0f, {}};
constexpr Material additive{Blending::additive, depth_read_only, FaceCulling::back_faces, 0.0f, {}};

constexpr std::array<float, 4> untinted{1.0f, 1.0f, 1.0f, 1.0f};
constexpr std::array<float, 4> black{0.0f, 0.0f, 0.0f, 1.0f};
constexpr Fog clear_air{black, 1000.0f, 2000.0f};
constexpr Vector3 light_from_above{0.0f, 1.0f, 0.0f};
constexpr Vector3 no_light{0.0f, 0.0f, 0.0f};

constexpr QuadPlacement whole_frame{-1.0f, -1.0f, 1.0f, 1.0f, 0.5f};
constexpr QuadPlacement left_half{-1.0f, -1.0f, 0.0f, 1.0f, 0.5f};
constexpr QuadPlacement right_half{0.0f, -1.0f, 1.0f, 1.0f, 0.5f};
constexpr QuadPlacement far_backdrop{-1.0f, -1.0f, 1.0f, 1.0f, 0.9f};

constexpr VkOffset2D left_center{16, 32};
constexpr VkOffset2D right_center{48, 32};
constexpr VkOffset2D left_upper_corner{4, 8};
constexpr VkOffset2D left_lower_corner{28, 56};
constexpr VkOffset2D right_upper_corner{36, 8};
constexpr VkOffset2D right_lower_corner{60, 56};

constexpr std::array<float, 2> opaque_texel{0.25f, 0.5f};
constexpr std::array<float, 2> transparent_texel{0.75f, 0.5f};
constexpr std::array<std::int16_t, 2> darkest_light{0, 0};
constexpr std::array<std::int16_t, 2> tinting_overlay_texel{3, 3};
constexpr std::uint8_t darkest_light_level = 128;
constexpr std::uint8_t overlay_transparency = 128;

constexpr std::array<std::uint8_t, 4> red{255, 0, 0, 255};
constexpr std::array<std::uint8_t, 4> green{0, 255, 0, 255};
constexpr std::array<std::uint8_t, 4> blue{0, 0, 255, 255};
constexpr std::array<std::uint8_t, 4> orange{255, 128, 0, 255};
constexpr std::array<std::uint8_t, 4> half_blue{0, 0, 255, 128};

SceneView view_through(const Fog& fog) {
    return SceneView{identity_matrix, fog, {light_from_above, no_light}};
}

std::vector<std::byte> texels(std::span<const std::uint8_t> channels) {
    const std::span<const std::byte> bytes = std::as_bytes(channels);
    return {bytes.begin(), bytes.end()};
}

std::vector<std::byte> surface_texels() {
    constexpr std::array<std::uint8_t, 8> opaque_then_transparent{255, 255, 255, 255, 255, 255, 255, 0};
    return texels(opaque_then_transparent);
}

std::vector<std::byte> lightmap_texels() {
    std::vector<std::uint8_t> channels(std::size_t{lightmap_extent.width} * lightmap_extent.height * 4, 255);
    channels[0] = darkest_light_level;
    channels[1] = darkest_light_level;
    channels[2] = darkest_light_level;
    return texels(channels);
}

std::vector<std::byte> overlay_texels() {
    std::vector<std::uint8_t> channels;
    for (std::uint32_t texel = 0; texel < overlay_extent.width * overlay_extent.height; ++texel) {
        channels.insert(channels.end(), {0, 0, 0, 255});
    }
    const std::size_t tinting =
        (static_cast<std::size_t>(tinting_overlay_texel[1]) * overlay_extent.width
            + static_cast<std::size_t>(tinting_overlay_texel[0]))
        * 4;
    channels[tinting] = 255;
    channels[tinting + 3] = overlay_transparency;
    return texels(channels);
}

SceneVertex colored(const std::array<std::uint8_t, 4>& color) {
    SceneVertex vertex;
    vertex.color = color;
    return vertex;
}

ImportedImage imported(
    const Image& image, VkImageAspectFlags aspect, const ImageUse& entry, const std::optional<ImageUse>& exit) {
    return ImportedImage{image.get(), image.view(), image.extent(), aspect, entry, exit};
}

class BatchScene {
public:
    BatchScene()
        : targets_(gpu_.device(), frame_extent),
          uploads_(gpu_.device(), upload_capacity),
          geometry_(gpu_.device(), geometry_capacity, geometry_arena_usage),
          stager_(geometry_),
          surface_(gpu_.device(), surface_extent, 1),
          lightmap_(gpu_.device(), lightmap_extent, 1),
          overlay_(gpu_.device(), overlay_extent, 1),
          samplers_(gpu_.device()),
          pipelines_(gpu_.device()),
          descriptors_(
              gpu_.device(),
              DescriptorPoolShape{
                  2,
                  {{VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, 1}, {VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 3}},
                  1,
              }),
          view_constants_(gpu_.device()),
          renderer_(gpu_.device(), pipelines_) {
        upload(surface_, surface_extent, surface_texels());
        upload(lightmap_, lightmap_extent, lightmap_texels());
        upload(overlay_, overlay_extent, overlay_texels());

        const VkSampler sampler = samplers_.sampler_for(nearest_sampler);
        view_ = renderer_.bind_view(
            descriptors_,
            view_constants_,
            ViewTextures{lightmap_.image().view(), sampler, overlay_.image().view(), sampler});
        surface_set_ = renderer_.bind_texture(descriptors_, surface_.image().view(), sampler);
    }

    void add(
        const VertexFormat& format,
        PrimitiveMode mode,
        std::span<const SceneVertex> vertices,
        const Material& material,
        const std::array<float, 4>& color_modulator = untinted) {
        const std::vector<std::byte> bytes = encode_vertices(format, vertices);
        const BatchGeometry staged = stager_.stage({format, mode, bytes}).value();
        batches_.push_back(DrawBatch{format, staged, material, surface_set_, identity_matrix, color_modulator});
    }

    void add_quad(const QuadPlacement& placement, const SceneVertex& surface, const Material& material) {
        add(entity_format, PrimitiveMode::quads, quad_corners(placement, surface), material);
    }

    FramePixels render(const SceneView& view) {
        view_constants_.write(view);

        RenderGraph graph;
        const ImageId color = graph.import_image(
            "frame color",
            imported(targets_.color(), VK_IMAGE_ASPECT_COLOR_BIT, uses::untouched, uses::transfer_source));
        const ImageId depth = graph.import_image(
            "frame depth", imported(targets_.depth(), VK_IMAGE_ASPECT_DEPTH_BIT, uses::untouched, std::nullopt));
        graph.add_pass(
            "batches",
            {{color, uses::color_attachment}, {depth, uses::depth_attachment}},
            [this, color, depth](VkCommandBuffer commands, const GraphImages& images) {
                begin_frame_rendering(commands, {images.at(color), images.at(depth)}, black);
                renderer_.record(commands, {frame_extent, view_}, batches_);
                vkCmdEndRendering(commands);
            });

        const PlannedFrame planned(gpu_.device(), std::move(graph));
        gpu_.device().submit([&planned](VkCommandBuffer commands) { planned.record(commands); });
        return read_back_color(gpu_.device(), targets_.color());
    }

    std::string validation_text() const {
        return gpu_.validation_text();
    }

private:
    void upload(const Texture& texture, VkExtent2D extent, const std::vector<std::byte>& rgba) {
        const MipChain chain = build_mip_chain(extent, rgba, 1);
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
    Texture surface_;
    Texture lightmap_;
    Texture overlay_;
    SamplerCache samplers_;
    PipelineCache pipelines_;
    DescriptorPools descriptors_;
    ViewConstants view_constants_;
    BatchRenderer renderer_;
    VkDescriptorSet view_ = VK_NULL_HANDLE;
    VkDescriptorSet surface_set_ = VK_NULL_HANDLE;
    std::vector<DrawBatch> batches_;
};

void check_pixel(const FramePixels& pixels, const VkOffset2D& position, const Rgb& expected) {
    const Pixel drawn = pixels.at(static_cast<std::uint32_t>(position.x), static_cast<std::uint32_t>(position.y));
    CHECK_MESSAGE(is_close(drawn, expected), describe(drawn));
}

int faded(float fog) {
    return static_cast<int>(std::lround(255.0f * (1.0f - fog)));
}

}

TEST_CASE("vertex colors fill geometry that carries no texture coordinates") {
    BatchScene scene;
    scene.add(colored_format, PrimitiveMode::quads, quad_corners(whole_frame, colored(orange)), opaque);

    const FramePixels pixels = scene.render(view_through(clear_air));

    check_pixel(pixels, left_center, {255, 128, 0});
    check_pixel(pixels, right_center, {255, 128, 0});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("the color modulator tints a whole batch") {
    BatchScene scene;
    scene.add(colored_format, PrimitiveMode::quads, quad_corners(whole_frame, {}), opaque, {0.5f, 1.0f, 0.25f, 1.0f});

    check_pixel(scene.render(view_through(clear_air)), left_center, {128, 255, 64});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("texels below the alpha cutoff leave what is behind them") {
    BatchScene scene;
    Material cutout = opaque;
    cutout.alpha_cutoff = 0.1f;
    cutout.shading.samples_texture = true;
    SceneVertex solid = colored(green);
    solid.texture_coordinate = opaque_texel;
    SceneVertex hole = colored(green);
    hole.texture_coordinate = transparent_texel;
    scene.add(textured_format, PrimitiveMode::quads, quad_corners(left_half, solid), cutout);
    scene.add(textured_format, PrimitiveMode::quads, quad_corners(right_half, hole), cutout);

    const FramePixels pixels = scene.render(view_through(clear_air));

    check_pixel(pixels, left_center, {0, 255, 0});
    check_pixel(pixels, right_center, {0, 0, 0});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("the lightmap scales a batch by the light packed into its vertices") {
    BatchScene scene;
    Material lit = opaque;
    lit.shading.applies_lightmap = true;
    SceneVertex dark = colored(red);
    dark.packed_light = darkest_light;
    scene.add_quad(left_half, dark, lit);
    scene.add_quad(right_half, colored(red), lit);

    const FramePixels pixels = scene.render(view_through(clear_air));

    check_pixel(pixels, left_center, {darkest_light_level, 0, 0});
    check_pixel(pixels, right_center, {255, 0, 0});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("the overlay table tints a batch toward its texel by the texel's transparency") {
    BatchScene scene;
    Material overlaid = opaque;
    overlaid.shading.applies_overlay = true;
    SceneVertex hurt;
    hurt.overlay_texel = tinting_overlay_texel;
    scene.add_quad(left_half, hurt, overlaid);
    scene.add_quad(right_half, {}, overlaid);

    const FramePixels pixels = scene.render(view_through(clear_air));

    check_pixel(pixels, left_center, {255, overlay_transparency, overlay_transparency});
    check_pixel(pixels, right_center, {255, 255, 255});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("normals facing a light are fully lit and normals facing away keep the ambient share") {
    BatchScene scene;
    Material shaded = opaque;
    shaded.shading.shades_by_normal = true;
    SceneVertex facing_away;
    facing_away.normal = {0, -127, 0, 0};
    scene.add_quad(left_half, {}, shaded);
    scene.add_quad(right_half, facing_away, shaded);

    const FramePixels pixels = scene.render(view_through(clear_air));

    check_pixel(pixels, left_center, {255, 255, 255});
    check_pixel(pixels, right_center, {102, 102, 102});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("translucent batches blend over what is behind them and additive ones brighten it") {
    BatchScene scene;
    scene.add_quad(far_backdrop, colored(red), opaque);
    scene.add_quad(left_half, colored(half_blue), translucent);
    scene.add_quad(right_half, colored(green), additive);

    const FramePixels pixels = scene.render(view_through(clear_air));

    check_pixel(pixels, left_center, {127, 0, 128});
    check_pixel(pixels, right_center, {255, 255, 0});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("nearer geometry stays in front of farther geometry submitted after it") {
    BatchScene scene;
    scene.add_quad(whole_frame, colored(green), opaque);
    scene.add_quad(far_backdrop, colored(red), opaque);

    check_pixel(scene.render(view_through(clear_air)), left_center, {0, 255, 0});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("fans and strips cover the area of the quads they describe") {
    BatchScene scene;
    const std::array<SceneVertex, 4> fan = quad_corners(left_half, colored(blue));
    const std::array<SceneVertex, 4> around = quad_corners(right_half, colored(green));
    const std::array<SceneVertex, 4> strip{around[0], around[1], around[3], around[2]};
    scene.add(colored_format, PrimitiveMode::triangle_fan, fan, opaque);
    scene.add(colored_format, PrimitiveMode::triangle_strip, strip, opaque);

    const FramePixels pixels = scene.render(view_through(clear_air));

    check_pixel(pixels, left_upper_corner, {0, 0, 255});
    check_pixel(pixels, left_lower_corner, {0, 0, 255});
    check_pixel(pixels, right_upper_corner, {0, 255, 0});
    check_pixel(pixels, right_lower_corner, {0, 255, 0});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("back faces are dropped unless the material keeps both sides") {
    BatchScene scene;
    Material two_sided = opaque;
    two_sided.culling = FaceCulling::disabled;
    const std::array<SceneVertex, 4> left = quad_corners(left_half, colored(red));
    const std::array<SceneVertex, 4> right = quad_corners(right_half, colored(red));
    const std::array<SceneVertex, 4> left_clockwise{left[3], left[2], left[1], left[0]};
    const std::array<SceneVertex, 4> right_clockwise{right[3], right[2], right[1], right[0]};
    scene.add(colored_format, PrimitiveMode::quads, left_clockwise, opaque);
    scene.add(colored_format, PrimitiveMode::quads, right_clockwise, two_sided);

    const FramePixels pixels = scene.render(view_through(clear_air));

    check_pixel(pixels, left_center, {0, 0, 0});
    check_pixel(pixels, right_center, {255, 0, 0});
    CHECK(scene.validation_text() == "");
}

TEST_CASE("fog fades a batch by the distance its shape measures") {
    constexpr Vector3 frame_corner{1.0f, 1.0f, 0.5f};
    constexpr std::array<float, 4> blue_haze{0.0f, 0.0f, 1.0f, 1.0f};
    Material fogged = opaque;
    fogged.shading.fades_into_fog = true;

    for (const FogShape shape : {FogShape::sphere, FogShape::cylinder}) {
        BatchScene scene;
        scene.add_quad(whole_frame, {}, fogged);
        const Fog haze{blue_haze, 0.0f, 2.0f, shape};
        const int remaining = faded(fog_factor(haze, fog_distance(shape, frame_corner)));

        check_pixel(scene.render(view_through(haze)), left_center, {remaining, remaining, 255});
        CHECK(scene.validation_text() == "");
    }
}
