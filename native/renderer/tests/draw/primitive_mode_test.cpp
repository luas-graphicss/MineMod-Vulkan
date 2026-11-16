#include <cstdint>
#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "draw/primitive_mode.hpp"

namespace {

using namespace minemod::renderer;

std::vector<std::uint32_t> triangle_indices(PrimitiveMode mode, std::uint32_t vertex_count) {
    std::vector<std::uint32_t> indices;
    append_triangle_indices(mode, vertex_count, indices);
    return indices;
}

}

TEST_CASE("each quad becomes two triangles that share its first and third corners") {
    CHECK(triangle_indices(PrimitiveMode::quads, 8) == std::vector<std::uint32_t>{0, 1, 2, 2, 3, 0, 4, 5, 6, 6, 7, 4});
    CHECK(triangle_index_count(PrimitiveMode::quads, 8) == 12);
}

TEST_CASE("a strip alternates its corner order so every triangle keeps the same winding") {
    CHECK(triangle_indices(PrimitiveMode::triangle_strip, 5) == std::vector<std::uint32_t>{0, 1, 2, 2, 1, 3, 2, 3, 4});
}

TEST_CASE("a fan anchors every triangle on its first vertex") {
    CHECK(triangle_indices(PrimitiveMode::triangle_fan, 5) == std::vector<std::uint32_t>{0, 1, 2, 0, 2, 3, 0, 3, 4});
}

TEST_CASE("separate triangles are drawn in the order they were submitted") {
    std::vector<std::uint32_t> indices{7};
    append_triangle_indices(PrimitiveMode::triangles, 6, indices);

    CHECK(indices == std::vector<std::uint32_t>{7, 0, 1, 2, 3, 4, 5});
}

TEST_CASE("vertices that do not form whole primitives are rejected") {
    CHECK_THROWS_AS(triangle_index_count(PrimitiveMode::quads, 6), std::invalid_argument);
    CHECK_THROWS_AS(triangle_index_count(PrimitiveMode::triangles, 4), std::invalid_argument);
    CHECK_THROWS_AS(triangle_index_count(PrimitiveMode::triangles, 0), std::invalid_argument);
    CHECK_THROWS_AS(triangle_index_count(PrimitiveMode::triangle_strip, 2), std::invalid_argument);
    CHECK_THROWS_AS(triangle_index_count(PrimitiveMode::triangle_fan, 2), std::invalid_argument);
}
