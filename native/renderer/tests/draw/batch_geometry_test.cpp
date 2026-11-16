#include <array>
#include <cstddef>
#include <optional>
#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "draw/batch_geometry.hpp"
#include "support/scene_vertices.hpp"
#include "support/validated_device.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr VertexFormat colored_format{VertexElement::position, VertexElement::color};
constexpr VkDeviceSize index_bytes = 4;

std::vector<std::byte> two_quads() {
    std::vector<SceneVertex> vertices;
    for (const SceneVertex& corner : quad_corners({-1.0f, -1.0f, 0.0f, 1.0f, 0.5f}, {})) {
        vertices.push_back(corner);
    }
    for (const SceneVertex& corner : quad_corners({0.0f, -1.0f, 1.0f, 1.0f, 0.5f}, {})) {
        vertices.push_back(corner);
    }
    return encode_vertices(colored_format, vertices);
}

}

TEST_CASE("a staged batch holds its vertices and the triangles of its primitives in the arena") {
    const ValidatedDevice gpu;
    UploadArena arena(gpu.device(), 1024, geometry_arena_usage);
    BatchStager stager(arena);
    const std::vector<std::byte> vertices = two_quads();

    const BatchGeometry staged = stager.stage({colored_format, PrimitiveMode::quads, vertices}).value();

    CHECK(staged.buffer == arena.buffer());
    CHECK(staged.vertices.size == vertices.size());
    CHECK(staged.index_count == 12);
    CHECK(staged.indices.size == staged.index_count * index_bytes);
    CHECK(staged.indices.offset % index_bytes == 0);
    CHECK(staged.indices.offset >= staged.vertices.offset + staged.vertices.size);
}

TEST_CASE("a batch that does not fit the arena is refused instead of overrunning it") {
    const ValidatedDevice gpu;
    UploadArena arena(gpu.device(), 96, geometry_arena_usage);
    BatchStager stager(arena);
    const std::vector<std::byte> vertices = two_quads();

    CHECK_FALSE(stager.stage({colored_format, PrimitiveMode::quads, vertices}).has_value());
    CHECK(arena.used() <= 96);
}

TEST_CASE("vertex bytes that are not whole vertices of the format are rejected") {
    std::vector<std::byte> vertices = two_quads();
    vertices.pop_back();

    CHECK_THROWS_AS(vertex_count({colored_format, PrimitiveMode::quads, vertices}), std::invalid_argument);
    CHECK_THROWS_AS(vertex_count({colored_format, PrimitiveMode::quads, {}}), std::invalid_argument);
}
