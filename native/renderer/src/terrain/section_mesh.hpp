#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <vector>

#include "terrain/terrain_layer.hpp"
#include "terrain/terrain_vertex.hpp"

namespace minemod::renderer {

struct IndexRange {
    std::uint32_t first_index;
    std::uint32_t index_count;

    friend bool operator==(const IndexRange&, const IndexRange&) = default;
};

struct SectionMesh {
    std::vector<TerrainVertex> vertices;
    std::vector<std::uint32_t> indices;
    std::array<IndexRange, terrain_layer_count> layers;
};

inline constexpr std::size_t max_section_vertices = std::size_t{1} << 20;
inline constexpr std::size_t max_section_indices = std::size_t{3} << 19;

void require_drawable(const SectionMesh& mesh);

}
