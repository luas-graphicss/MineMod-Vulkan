#include "draw/batch_geometry.hpp"

#include <stdexcept>

namespace minemod::renderer {
namespace {

constexpr VkDeviceSize geometry_alignment = 4;

}

std::uint32_t vertex_count(const BatchVertices& vertices) {
    const std::size_t stride = vertices.format.stride();
    if (vertices.bytes.empty() || vertices.bytes.size() % stride != 0) {
        throw std::invalid_argument("the vertex bytes do not hold whole vertices of their format");
    }
    const std::size_t count = vertices.bytes.size() / stride;
    if (count > max_batch_vertices) {
        throw std::invalid_argument("a batch exceeds the vertex limit");
    }
    return static_cast<std::uint32_t>(count);
}

std::optional<BatchGeometry> BatchStager::stage(const BatchVertices& vertices) {
    indices_.clear();
    append_triangle_indices(vertices.mode, vertex_count(vertices), indices_);

    const std::optional<ArenaRegion> staged_vertices = arena_.stage(vertices.bytes, geometry_alignment);
    if (!staged_vertices) {
        return std::nullopt;
    }
    const std::optional<ArenaRegion> staged_indices =
        arena_.stage(std::as_bytes(std::span(indices_)), geometry_alignment);
    if (!staged_indices) {
        return std::nullopt;
    }
    return BatchGeometry{
        arena_.buffer(), *staged_vertices, *staged_indices, static_cast<std::uint32_t>(indices_.size())};
}

}
