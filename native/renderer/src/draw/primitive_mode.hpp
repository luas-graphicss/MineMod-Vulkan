#pragma once

#include <cstdint>
#include <vector>

namespace minemod::renderer {

enum class PrimitiveMode : std::uint8_t { triangles, triangle_strip, triangle_fan, quads, lines };

std::uint32_t triangle_index_count(PrimitiveMode mode, std::uint32_t vertex_count);

void append_triangle_indices(PrimitiveMode mode, std::uint32_t vertex_count, std::vector<std::uint32_t>& indices);

}
