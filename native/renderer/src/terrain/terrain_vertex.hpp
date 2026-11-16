#pragma once

#include <array>
#include <cstdint>

#include "pipelines/graphics_pipeline_state.hpp"

namespace minemod::renderer {

struct TerrainVertex {
    std::array<float, 3> position;
    std::array<std::uint8_t, 4> color;
    std::array<float, 2> texture_coordinate;
    std::array<std::uint16_t, 2> packed_light;
};

inline constexpr std::uint16_t brightest_packed_light = 240;

VertexLayout terrain_vertex_layout();

}
