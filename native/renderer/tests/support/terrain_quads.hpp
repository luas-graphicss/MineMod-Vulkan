#pragma once

#include <array>
#include <cstdint>

#include "terrain/section_mesh.hpp"
#include "terrain/terrain_layer.hpp"

namespace minemod::renderer::test_support {

struct QuadPlacement {
    float left;
    float bottom;
    float right;
    float top;
    float depth;
};

struct QuadSurface {
    std::array<std::uint8_t, 4> color;
    std::array<float, 2> texture_coordinate;
    std::array<std::uint16_t, 2> packed_light;
};

void append_quad(SectionMesh& mesh, TerrainLayer layer, const QuadPlacement& placement, const QuadSurface& surface);

}
