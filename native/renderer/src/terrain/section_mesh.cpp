#include "terrain/section_mesh.hpp"

#include <algorithm>
#include <stdexcept>

namespace minemod::renderer {
namespace {

constexpr std::uint32_t indices_per_triangle = 3;

bool fits(const IndexRange& range, std::size_t index_count) noexcept {
    return range.first_index <= index_count && range.index_count <= index_count - range.first_index;
}

void require_triangles_inside(const IndexRange& range, std::size_t index_count) {
    if (!fits(range, index_count)) {
        throw std::invalid_argument("a terrain layer reaches past the indices of its section mesh");
    }
    if (range.index_count % indices_per_triangle != 0) {
        throw std::invalid_argument("a terrain layer must hold whole triangles");
    }
}

}

void require_drawable(const SectionMesh& mesh) {
    if (mesh.vertices.empty() || mesh.indices.empty()) {
        throw std::invalid_argument("a section mesh needs vertices and indices; unload a section that has none");
    }
    if (mesh.vertices.size() > max_section_vertices || mesh.indices.size() > max_section_indices) {
        throw std::invalid_argument("a section mesh exceeds the vertex or index limit of one section");
    }

    const std::size_t vertex_count = mesh.vertices.size();
    if (std::ranges::any_of(mesh.indices, [vertex_count](std::uint32_t index) { return index >= vertex_count; })) {
        throw std::invalid_argument("a section mesh index points past its vertices");
    }

    for (const IndexRange& layer : mesh.layers) {
        require_triangles_inside(layer, mesh.indices.size());
    }
}

}
