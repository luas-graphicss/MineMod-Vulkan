#include "support/terrain_quads.hpp"

#include <stdexcept>

namespace minemod::renderer::test_support {
namespace {

constexpr std::array<std::uint32_t, 6> quad_corners{0, 1, 2, 0, 2, 3};

}

void append_quad(SectionMesh& mesh, TerrainLayer layer, const QuadPlacement& placement, const QuadSurface& surface) {
    IndexRange& range = mesh.layers[layer_index(layer)];
    const auto first_new_index = static_cast<std::uint32_t>(mesh.indices.size());
    if (range.index_count == 0) {
        range.first_index = first_new_index;
    } else if (range.first_index + range.index_count != first_new_index) {
        throw std::logic_error("quads of one terrain layer must be appended together");
    }

    const auto first_vertex = static_cast<std::uint32_t>(mesh.vertices.size());
    const std::array<std::array<float, 2>, 4> counter_clockwise{{
        {placement.left, placement.bottom},
        {placement.right, placement.bottom},
        {placement.right, placement.top},
        {placement.left, placement.top},
    }};
    for (const std::array<float, 2>& corner : counter_clockwise) {
        mesh.vertices.push_back(TerrainVertex{
            {corner[0], corner[1], placement.depth},
            surface.color,
            surface.texture_coordinate,
            surface.packed_light,
        });
    }
    for (const std::uint32_t corner : quad_corners) {
        mesh.indices.push_back(first_vertex + corner);
    }
    range.index_count += static_cast<std::uint32_t>(quad_corners.size());
}

}
