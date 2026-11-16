#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

#include "draw/vertex_format.hpp"
#include "support/terrain_quads.hpp"

namespace minemod::renderer::test_support {

struct SceneVertex {
    std::array<float, 3> position{};
    std::array<std::uint8_t, 4> color{255, 255, 255, 255};
    std::array<float, 2> texture_coordinate{0.0f, 0.0f};
    std::array<std::int16_t, 2> overlay_texel{0, 10};
    std::array<std::int16_t, 2> packed_light{240, 240};
    std::array<std::int8_t, 4> normal{0, 127, 0, 0};
};

std::vector<std::byte> encode_vertices(const VertexFormat& format, std::span<const SceneVertex> vertices);

std::array<SceneVertex, 4> quad_corners(const QuadPlacement& placement, const SceneVertex& surface);

}
