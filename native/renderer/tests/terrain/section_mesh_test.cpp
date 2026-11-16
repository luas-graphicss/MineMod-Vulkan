#include <cstdint>
#include <limits>
#include <stdexcept>

#include <doctest.h>

#include "support/terrain_quads.hpp"
#include "terrain/section_mesh.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr QuadPlacement unit_quad{0.0f, 0.0f, 1.0f, 1.0f, 0.0f};
constexpr QuadSurface white{{255, 255, 255, 255}, {0.0f, 0.0f}, {brightest_packed_light, brightest_packed_light}};

SectionMesh single_quad() {
    SectionMesh mesh{};
    append_quad(mesh, TerrainLayer::solid, unit_quad, white);
    return mesh;
}

}

TEST_CASE("a mesh whose layers hold whole triangles inside its indices is drawable") {
    CHECK_NOTHROW(require_drawable(single_quad()));
}

TEST_CASE("a mesh without geometry is rejected") {
    CHECK_THROWS_AS(require_drawable(SectionMesh{}), std::invalid_argument);
}

TEST_CASE("an index past the vertices is rejected") {
    SectionMesh mesh = single_quad();
    mesh.indices.back() = static_cast<std::uint32_t>(mesh.vertices.size());

    CHECK_THROWS_AS(require_drawable(mesh), std::invalid_argument);
}

TEST_CASE("a layer that reaches past the indices is rejected") {
    SectionMesh mesh = single_quad();

    mesh.layers[layer_index(TerrainLayer::translucent)] = IndexRange{3, 6};
    CHECK_THROWS_AS(require_drawable(mesh), std::invalid_argument);

    mesh.layers[layer_index(TerrainLayer::translucent)] = IndexRange{std::numeric_limits<std::uint32_t>::max(), 3};
    CHECK_THROWS_AS(require_drawable(mesh), std::invalid_argument);
}

TEST_CASE("a layer that splits a triangle is rejected") {
    SectionMesh mesh = single_quad();
    mesh.layers[layer_index(TerrainLayer::solid)].index_count = 4;

    CHECK_THROWS_AS(require_drawable(mesh), std::invalid_argument);
}
