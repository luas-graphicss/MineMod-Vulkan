#include <array>
#include <cstdint>

#include <doctest.h>

#include "draw/batch_renderer.hpp"
#include "draw/material.hpp"
#include "draw/vertex_format.hpp"
#include "frame/frame_readback.hpp"
#include "support/batch_stage.hpp"
#include "support/pixel_colors.hpp"
#include "support/scene_vertices.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VertexFormat textured_format{VertexElement::position, VertexElement::color, VertexElement::texture};
constexpr DepthState depth_tested{true, true, VK_COMPARE_OP_LESS_OR_EQUAL};
constexpr Material textured{Blending::disabled, depth_tested, FaceCulling::back_faces, 0.0f, {.samples_texture = true}};
constexpr QuadPlacement left_half{-1.0f, -1.0f, 0.0f, 1.0f, 0.5f};
constexpr QuadPlacement right_half{0.0f, -1.0f, 1.0f, 1.0f, 0.5f};
constexpr float half_texture_width = 0.5f;
constexpr VkOffset2D left_center{16, 32};
constexpr VkOffset2D right_center{48, 32};

std::array<SceneVertex, 4> sampling_the_red_texel(const QuadPlacement& placement) {
    SceneVertex surface;
    surface.texture_coordinate = red_texel;
    return quad_corners(placement, surface);
}

void check_pixel(const FramePixels& pixels, const VkOffset2D& position, const Rgb& expected) {
    const Pixel drawn = pixels.at(static_cast<std::uint32_t>(position.x), static_cast<std::uint32_t>(position.y));
    CHECK_MESSAGE(is_close(drawn, expected), describe(drawn));
}

}

TEST_CASE("a texture transform moves the texel a batch samples without touching other batches") {
    BatchStage stage;
    DrawBatch scrolled = stage.quads(textured_format, sampling_the_red_texel(right_half), textured);
    scrolled.texture_transform.u_row[2] = half_texture_width;
    stage.draw(stage.quads(textured_format, sampling_the_red_texel(left_half), textured));
    stage.draw(scrolled);

    const FramePixels pixels = stage.render();

    check_pixel(pixels, left_center, {255, 0, 0});
    check_pixel(pixels, right_center, {0, 255, 0});
    CHECK(stage.validation_text() == "");
}
