#include <array>
#include <cstddef>
#include <cstdint>
#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "draw/batch_geometry.hpp"
#include "draw/material.hpp"
#include "draw/primitive_mode.hpp"
#include "draw/quad_order.hpp"
#include "draw/vertex_format.hpp"
#include "frame/frame_readback.hpp"
#include "support/batch_stage.hpp"
#include "support/pixel_colors.hpp"
#include "support/scene_vertices.hpp"
#include "support/validated_device.hpp"
#include "upload/upload_arena.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VertexFormat colored_format{VertexElement::position, VertexElement::color};
constexpr DepthState depth_tested{true, true, VK_COMPARE_OP_LESS_OR_EQUAL};
constexpr DepthState depth_read_only{true, false, VK_COMPARE_OP_LESS_OR_EQUAL};
constexpr DepthState depth_ignored{false, false, VK_COMPARE_OP_ALWAYS};
constexpr Material opaque{Blending::disabled, depth_tested, FaceCulling::back_faces, 0.0f, {}};
constexpr Material translucent{Blending::alpha, depth_read_only, FaceCulling::back_faces, 0.0f, {}};
constexpr Material interface_layer{Blending::alpha, depth_ignored, FaceCulling::back_faces, 0.0f, {}};

constexpr std::array<std::uint8_t, 4> red{255, 0, 0, 255};
constexpr std::array<std::uint8_t, 4> half_yellow{255, 255, 0, 128};
constexpr std::array<std::uint8_t, 4> half_blue{0, 0, 255, 128};
constexpr float backdrop_depth = 0.9f;
constexpr float far_depth = 0.7f;
constexpr float near_depth = 0.3f;
constexpr VkOffset2D frame_center{32, 32};
constexpr VkDeviceSize geometry_capacity = 1024;
constexpr Rgb yellow_under_blue{127, 64, 128};
constexpr Rgb blue_under_yellow{191, 128, 64};

std::array<SceneVertex, 4> pane(float depth, const std::array<std::uint8_t, 4>& color) {
    SceneVertex surface;
    surface.color = color;
    return quad_corners({-1.0f, -1.0f, 1.0f, 1.0f, depth}, surface);
}

std::vector<SceneVertex> panes(const std::array<SceneVertex, 4>& first, const std::array<SceneVertex, 4>& second) {
    std::vector<SceneVertex> corners(first.begin(), first.end());
    corners.insert(corners.end(), second.begin(), second.end());
    return corners;
}

Pixel center_over_a_red_backdrop(const std::vector<SceneVertex>& corners, const Material& material, QuadOrder order) {
    BatchStage stage;
    stage.draw(stage.quads(colored_format, pane(backdrop_depth, red), opaque));
    stage.draw(stage.quads(colored_format, corners, material, order));
    const FramePixels pixels = stage.render();
    CHECK(stage.validation_text() == "");
    return pixels.at(static_cast<std::uint32_t>(frame_center.x), static_cast<std::uint32_t>(frame_center.y));
}

void check_pixel(const Pixel& drawn, const Rgb& expected) {
    CHECK_MESSAGE(is_close(drawn, expected), describe(drawn));
}

}

TEST_CASE("translucent quads submitted nearest first blend from the farthest when they are ordered by distance") {
    const std::vector<SceneVertex> near_then_far = panes(pane(near_depth, half_blue), pane(far_depth, half_yellow));

    check_pixel(center_over_a_red_backdrop(near_then_far, translucent, QuadOrder::submitted), blue_under_yellow);
    check_pixel(center_over_a_red_backdrop(near_then_far, translucent, QuadOrder::farthest_first), yellow_under_blue);
}

TEST_CASE("interface quads are layered by their depth however they were submitted") {
    const std::vector<SceneVertex> high_then_low = panes(pane(far_depth, half_yellow), pane(near_depth, half_blue));

    check_pixel(center_over_a_red_backdrop(high_then_low, interface_layer, QuadOrder::submitted), yellow_under_blue);
    check_pixel(center_over_a_red_backdrop(high_then_low, interface_layer, QuadOrder::rising_depth), blue_under_yellow);
}

TEST_CASE("only quads can be drawn in another order than they were submitted") {
    const ValidatedDevice gpu;
    const std::vector<std::byte> triangle = encode_vertices(colored_format, std::array<SceneVertex, 3>{});
    UploadArena arena(gpu.device(), geometry_capacity, geometry_arena_usage);
    BatchStager stager(arena);

    CHECK_THROWS_AS(
        stager.stage({colored_format, PrimitiveMode::triangles, triangle}, QuadOrder::farthest_first),
        std::invalid_argument);
}
