#include <array>
#include <cstdint>

#include <doctest.h>

#include "draw/batch_renderer.hpp"
#include "draw/material.hpp"
#include "draw/vertex_format.hpp"
#include "frame/frame_readback.hpp"
#include "interface/clip_rectangle.hpp"
#include "support/batch_stage.hpp"
#include "support/pixel_colors.hpp"
#include "support/scene_vertices.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VertexFormat colored_format{VertexElement::position, VertexElement::color};
constexpr DepthState depth_ignored{false, false, VK_COMPARE_OP_ALWAYS};
constexpr Material interface_fill{Blending::disabled, depth_ignored, FaceCulling::back_faces, 0.0f, {}};
constexpr QuadPlacement whole_frame{-1.0f, -1.0f, 1.0f, 1.0f, 0.5f};
constexpr std::array<std::uint8_t, 4> green{0, 255, 0, 255};
constexpr LowerLeftRectangle lowest_quarter{0, 0, 64, 16};
constexpr LowerLeftRectangle outside_the_frame{64, 0, 16, 16};
constexpr VkOffset2D near_the_top{32, 8};
constexpr VkOffset2D near_the_bottom{32, 56};

FramePixels frame_filled_inside(const LowerLeftRectangle& rectangle) {
    BatchStage stage;
    SceneVertex surface;
    surface.color = green;
    DrawBatch fill = stage.quads(colored_format, quad_corners(whole_frame, surface), interface_fill);
    fill.clip = clip_rectangle(rectangle, stage.extent());
    stage.draw(fill);

    const FramePixels pixels = stage.render();
    CHECK(stage.validation_text() == "");
    return pixels;
}

void check_pixel(const FramePixels& pixels, const VkOffset2D& position, const Rgb& expected) {
    const Pixel drawn = pixels.at(static_cast<std::uint32_t>(position.x), static_cast<std::uint32_t>(position.y));
    CHECK_MESSAGE(is_close(drawn, expected), describe(drawn));
}

}

TEST_CASE("a clipped batch only touches the pixels inside its rectangle") {
    const FramePixels pixels = frame_filled_inside(lowest_quarter);

    check_pixel(pixels, near_the_bottom, {0, 255, 0});
    check_pixel(pixels, near_the_top, {0, 0, 0});
}

TEST_CASE("a batch clipped to a rectangle outside the frame draws nothing") {
    const FramePixels pixels = frame_filled_inside(outside_the_frame);

    check_pixel(pixels, near_the_bottom, {0, 0, 0});
    check_pixel(pixels, near_the_top, {0, 0, 0});
}

TEST_CASE("a clip stays with its batch and the next batch fills the whole frame") {
    BatchStage stage;
    SceneVertex surface;
    surface.color = green;
    DrawBatch clipped = stage.quads(colored_format, quad_corners(whole_frame, surface), interface_fill);
    clipped.clip = clip_rectangle(outside_the_frame, stage.extent());
    stage.draw(clipped);
    stage.draw(stage.quads(colored_format, quad_corners(whole_frame, surface), interface_fill));

    check_pixel(stage.render(), near_the_top, {0, 255, 0});
    CHECK(stage.validation_text() == "");
}
