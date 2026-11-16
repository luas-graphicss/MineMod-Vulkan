#include "draw/primitive_mode.hpp"

#include <array>
#include <stdexcept>

namespace minemod::renderer {
namespace {

constexpr std::uint32_t triangle_corners = 3;
constexpr std::uint32_t quad_corners = 4;
constexpr std::array<std::uint32_t, 6> quad_triangles{0, 1, 2, 2, 3, 0};

void require_whole_primitives(std::uint32_t vertex_count, std::uint32_t corners) {
    if (vertex_count == 0 || vertex_count % corners != 0) {
        throw std::invalid_argument("the vertices do not form whole primitives");
    }
}

void require_connected_triangle(std::uint32_t vertex_count) {
    if (vertex_count < triangle_corners) {
        throw std::invalid_argument("a strip or fan needs at least one triangle");
    }
}

void append_quads(std::uint32_t vertex_count, std::vector<std::uint32_t>& indices) {
    for (std::uint32_t first = 0; first < vertex_count; first += quad_corners) {
        for (const std::uint32_t corner : quad_triangles) {
            indices.push_back(first + corner);
        }
    }
}

void append_strip(std::uint32_t vertex_count, std::vector<std::uint32_t>& indices) {
    for (std::uint32_t first = 0; first + 2 < vertex_count; ++first) {
        const bool keeps_winding = first % 2 == 0;
        indices.push_back(keeps_winding ? first : first + 1);
        indices.push_back(keeps_winding ? first + 1 : first);
        indices.push_back(first + 2);
    }
}

void append_fan(std::uint32_t vertex_count, std::vector<std::uint32_t>& indices) {
    for (std::uint32_t outer = 1; outer + 1 < vertex_count; ++outer) {
        indices.push_back(0);
        indices.push_back(outer);
        indices.push_back(outer + 1);
    }
}

}

std::uint32_t triangle_index_count(PrimitiveMode mode, std::uint32_t vertex_count) {
    switch (mode) {
    case PrimitiveMode::quads:
        require_whole_primitives(vertex_count, quad_corners);
        return vertex_count / quad_corners * static_cast<std::uint32_t>(quad_triangles.size());
    case PrimitiveMode::triangle_strip:
    case PrimitiveMode::triangle_fan:
        require_connected_triangle(vertex_count);
        return (vertex_count - 2) * triangle_corners;
    case PrimitiveMode::triangles:
        break;
    }
    require_whole_primitives(vertex_count, triangle_corners);
    return vertex_count;
}

void append_triangle_indices(PrimitiveMode mode, std::uint32_t vertex_count, std::vector<std::uint32_t>& indices) {
    indices.reserve(indices.size() + triangle_index_count(mode, vertex_count));
    switch (mode) {
    case PrimitiveMode::quads:
        append_quads(vertex_count, indices);
        return;
    case PrimitiveMode::triangle_strip:
        append_strip(vertex_count, indices);
        return;
    case PrimitiveMode::triangle_fan:
        append_fan(vertex_count, indices);
        return;
    case PrimitiveMode::triangles:
        break;
    }
    for (std::uint32_t vertex = 0; vertex < vertex_count; ++vertex) {
        indices.push_back(vertex);
    }
}

}
