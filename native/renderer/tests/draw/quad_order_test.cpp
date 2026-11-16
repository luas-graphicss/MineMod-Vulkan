#include <array>
#include <cstddef>
#include <cstdint>
#include <initializer_list>
#include <span>
#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "draw/primitive_mode.hpp"
#include "draw/quad_order.hpp"
#include "draw/vertex_format.hpp"
#include "support/scene_vertices.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VertexFormat position_first{VertexElement::position, VertexElement::color};
constexpr VertexFormat position_last{VertexElement::color, VertexElement::texture, VertexElement::position};
constexpr std::uint32_t corners_per_quad = 4;
constexpr std::size_t indices_per_quad = 6;

std::vector<std::byte> panes(const VertexFormat& format, std::initializer_list<float> depths) {
    std::vector<SceneVertex> corners;
    for (const float depth : depths) {
        const std::array<SceneVertex, 4> pane = quad_corners({-1.0f, -1.0f, 1.0f, 1.0f, depth}, {});
        corners.insert(corners.end(), pane.begin(), pane.end());
    }
    return encode_vertices(format, corners);
}

std::vector<std::uint32_t> submitted_triangles(std::uint32_t quad_count) {
    std::vector<std::uint32_t> triangles;
    append_triangle_indices(PrimitiveMode::quads, quad_count * corners_per_quad, triangles);
    return triangles;
}

std::vector<std::uint32_t> first_corners(const std::vector<std::uint32_t>& triangles) {
    std::vector<std::uint32_t> corners;
    for (std::size_t first = 0; first < triangles.size(); first += indices_per_quad) {
        corners.push_back(triangles[first]);
    }
    return corners;
}

std::vector<std::uint32_t> drawn_first_corners(
    const VertexFormat& format, std::initializer_list<float> depths, QuadOrder order) {
    const std::vector<std::byte> vertices = panes(format, depths);
    std::vector<std::uint32_t> triangles = submitted_triangles(static_cast<std::uint32_t>(depths.size()));
    order_quads(format, vertices, order, triangles);
    return first_corners(triangles);
}

}

TEST_CASE("quads farther from the origin are drawn first") {
    CHECK(
        drawn_first_corners(position_first, {-0.2f, -0.9f, -0.5f}, QuadOrder::farthest_first)
        == std::vector<std::uint32_t>{4, 8, 0});
}

TEST_CASE("interface quads are drawn by rising depth") {
    CHECK(
        drawn_first_corners(position_first, {0.3f, 0.1f, 0.2f}, QuadOrder::rising_depth)
        == std::vector<std::uint32_t>{4, 8, 0});
}

TEST_CASE("quads that tie keep the order they were submitted in") {
    CHECK(
        drawn_first_corners(position_first, {0.5f, 0.5f, 0.1f}, QuadOrder::rising_depth)
        == std::vector<std::uint32_t>{8, 0, 4});
}

TEST_CASE("the submitted order leaves the triangles alone") {
    CHECK(
        drawn_first_corners(position_first, {0.3f, 0.1f, 0.2f}, QuadOrder::submitted)
        == std::vector<std::uint32_t>{0, 4, 8});
}

TEST_CASE("the position is read wherever the format places it") {
    CHECK(
        drawn_first_corners(position_last, {0.1f, 0.8f}, QuadOrder::farthest_first)
        == std::vector<std::uint32_t>{4, 0});
}

TEST_CASE("triangles that do not belong to the quads of the vertices are rejected") {
    const std::vector<std::byte> one_pane = panes(position_first, {0.5f});
    std::vector<std::uint32_t> two_quads = submitted_triangles(2);
    std::vector<std::uint32_t> split_quad = submitted_triangles(1);
    split_quad.pop_back();

    CHECK_THROWS_AS(order_quads(position_first, one_pane, QuadOrder::farthest_first, two_quads), std::invalid_argument);
    CHECK_THROWS_AS(order_quads(position_first, one_pane, QuadOrder::submitted, split_quad), std::invalid_argument);
}
