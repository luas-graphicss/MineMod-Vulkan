#include <cstddef>
#include <cstdint>
#include <initializer_list>
#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "math/vector.hpp"
#include "support/terrain_quads.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/translucent_order.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr QuadSurface white{{255, 255, 255, 255}, {0.0f, 0.0f}, {brightest_packed_light, brightest_packed_light}};
constexpr Vector3 camera_in_front{8.0f, 8.0f, 20.0f};

void append_pane(SectionMesh& mesh, TerrainLayer layer, float depth) {
    append_quad(mesh, layer, {0.0f, 0.0f, 16.0f, 16.0f, depth}, white);
}

SectionMesh translucent_panes(std::initializer_list<float> depths) {
    SectionMesh mesh{};
    for (const float depth : depths) {
        append_pane(mesh, TerrainLayer::translucent, depth);
    }
    return mesh;
}

std::vector<std::uint32_t> first_corners(const std::vector<std::uint32_t>& indices) {
    std::vector<std::uint32_t> corners;
    for (std::size_t first = 0; first < indices.size(); first += indices_per_quad) {
        corners.push_back(indices[first]);
    }
    return corners;
}

}

TEST_CASE("translucent quads are ordered from the farthest to the nearest of the camera") {
    const TranslucentQuads quads = translucent_quads(translucent_panes({12.0f, 2.0f, 7.0f}));

    CHECK(first_corners(far_to_near(quads, camera_in_front)) == std::vector<std::uint32_t>{4, 8, 0});
}

TEST_CASE("quads at the same distance keep the order of the mesh") {
    const TranslucentQuads quads = translucent_quads(translucent_panes({5.0f, 5.0f, 9.0f}));

    CHECK(first_corners(far_to_near(quads, camera_in_front)) == std::vector<std::uint32_t>{0, 4, 8});
}

TEST_CASE("only the translucent layer of a mesh is ordered") {
    SectionMesh mesh{};
    append_pane(mesh, TerrainLayer::solid, 1.0f);
    append_pane(mesh, TerrainLayer::translucent, 12.0f);
    append_pane(mesh, TerrainLayer::translucent, 3.0f);

    const TranslucentQuads quads = translucent_quads(mesh);

    CHECK(quads.centers.size() == 2);
    CHECK(quads.centers.front() == Vector3{8.0f, 8.0f, 12.0f});
    CHECK(first_corners(far_to_near(quads, camera_in_front)) == std::vector<std::uint32_t>{8, 4});
}

TEST_CASE("a mesh without translucent geometry has nothing to order") {
    SectionMesh mesh{};
    append_pane(mesh, TerrainLayer::solid, 1.0f);

    const TranslucentQuads quads = translucent_quads(mesh);

    CHECK(quads.centers.empty());
    CHECK(far_to_near(quads, camera_in_front).empty());
}

TEST_CASE("a translucent layer that splits a quad is rejected") {
    SectionMesh mesh = translucent_panes({4.0f});
    mesh.layers[layer_index(TerrainLayer::translucent)].index_count = 3;

    CHECK_THROWS_AS(translucent_quads(mesh), std::invalid_argument);
}

TEST_CASE("quads whose indices do not match their centers are rejected") {
    TranslucentQuads quads = translucent_quads(translucent_panes({4.0f, 8.0f}));
    quads.indices.pop_back();

    CHECK_THROWS_AS(far_to_near(quads, camera_in_front), std::invalid_argument);
}
