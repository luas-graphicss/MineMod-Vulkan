#include <array>
#include <cstdint>

#include <doctest.h>

#include "draw/material.hpp"
#include "draw/vertex_format.hpp"
#include "frame/frame_readback.hpp"
#include "support/batch_stage.hpp"
#include "support/pixel_colors.hpp"
#include "support/scene_vertices.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VertexFormat colored_format{VertexElement::position, VertexElement::color};
constexpr DepthState depth_ignored{false, false, VK_COMPARE_OP_ALWAYS};
constexpr Material backdrop{Blending::disabled, depth_ignored, FaceCulling::back_faces, 0.0f, {}};
constexpr Material crosshair{Blending::inversion, depth_ignored, FaceCulling::back_faces, 0.0f, {}};
constexpr Material vignette{Blending::darkening, depth_ignored, FaceCulling::back_faces, 0.0f, {}};
constexpr QuadPlacement whole_frame{-1.0f, -1.0f, 1.0f, 1.0f, 0.5f};
constexpr std::array<std::uint8_t, 4> red{255, 0, 0, 255};
constexpr std::array<std::uint8_t, 4> white{255, 255, 255, 255};
constexpr std::array<std::uint8_t, 4> half_gray{128, 128, 128, 255};
constexpr std::uint32_t center = 32;

Pixel center_of(
    const std::array<std::uint8_t, 4>& behind, const std::array<std::uint8_t, 4>& over, const Material& material) {
    BatchStage stage;
    SceneVertex surface;
    surface.color = behind;
    stage.draw(stage.quads(colored_format, quad_corners(whole_frame, surface), backdrop));
    surface.color = over;
    stage.draw(stage.quads(colored_format, quad_corners(whole_frame, surface), material));

    const FramePixels pixels = stage.render();
    CHECK(stage.validation_text() == "");
    return pixels.at(center, center);
}

void check_pixel(const Pixel& drawn, const Rgb& expected) {
    CHECK_MESSAGE(is_close(drawn, expected), describe(drawn));
}

}

TEST_CASE("a white crosshair shows the opposite of the color behind it") {
    check_pixel(center_of(red, white, crosshair), {0, 255, 255});
    check_pixel(center_of(white, white, crosshair), {0, 0, 0});
}

TEST_CASE("a vignette darkens what is behind it by its own brightness") {
    check_pixel(center_of(white, half_gray, vignette), {127, 127, 127});
    check_pixel(center_of(red, white, vignette), {0, 0, 0});
}
