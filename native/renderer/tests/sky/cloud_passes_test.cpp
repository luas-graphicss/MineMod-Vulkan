#include <array>
#include <cstdint>
#include <vector>

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
constexpr DepthState depth_tested{true, true, VK_COMPARE_OP_LESS_OR_EQUAL};
constexpr DepthState depth_read_only{true, false, VK_COMPARE_OP_LESS_OR_EQUAL};
constexpr Material sky{Blending::disabled, depth_read_only, FaceCulling::back_faces, 0.0f, {}};
constexpr Material cloud_depth{
    .blending = Blending::disabled,
    .depth = depth_tested,
    .culling = FaceCulling::back_faces,
    .alpha_cutoff = 0.0f,
    .shading = {},
    .writes_color = false,
};
constexpr Material cloud_color{Blending::alpha, depth_read_only, FaceCulling::back_faces, 0.0f, {}};

constexpr std::array<std::uint8_t, 4> sky_blue{0, 0, 255, 255};
constexpr std::array<std::uint8_t, 4> half_white{255, 255, 255, 128};
constexpr float sky_depth = 0.9f;
constexpr float far_cloud_face = 0.6f;
constexpr float near_cloud_face = 0.4f;
constexpr std::uint32_t center = 32;

std::array<SceneVertex, 4> pane(float depth, const std::array<std::uint8_t, 4>& color) {
    SceneVertex surface;
    surface.color = color;
    return quad_corners({-1.0f, -1.0f, 1.0f, 1.0f, depth}, surface);
}

std::vector<SceneVertex> overlapping_cloud_faces() {
    const std::array<SceneVertex, 4> far_face = pane(far_cloud_face, half_white);
    const std::array<SceneVertex, 4> near_face = pane(near_cloud_face, half_white);
    std::vector<SceneVertex> faces(far_face.begin(), far_face.end());
    faces.insert(faces.end(), near_face.begin(), near_face.end());
    return faces;
}

enum class DepthPass { skipped, drawn_first };

Pixel center_of_clouds_over_the_sky(DepthPass depth_pass) {
    BatchStage stage;
    const std::vector<SceneVertex> faces = overlapping_cloud_faces();
    stage.draw(stage.quads(colored_format, pane(sky_depth, sky_blue), sky));
    if (depth_pass == DepthPass::drawn_first) {
        stage.draw(stage.quads(colored_format, faces, cloud_depth));
    }
    stage.draw(stage.quads(colored_format, faces, cloud_color));

    const FramePixels pixels = stage.render();
    CHECK(stage.validation_text() == "");
    return pixels.at(center, center);
}

void check_pixel(const Pixel& drawn, const Rgb& expected) {
    CHECK_MESSAGE(is_close(drawn, expected), describe(drawn));
}

}

TEST_CASE("overlapping cloud faces blend over the sky once after a pass that only writes their depth") {
    check_pixel(center_of_clouds_over_the_sky(DepthPass::drawn_first), {128, 128, 255});
}

TEST_CASE("without the depth pass every overlapping cloud face blends over the sky") {
    check_pixel(center_of_clouds_over_the_sky(DepthPass::skipped), {192, 192, 255});
}
