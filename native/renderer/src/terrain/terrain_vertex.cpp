#include "terrain/terrain_vertex.hpp"

#include <cstddef>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

static_assert(sizeof(TerrainVertex) == 28, "terrain vertices are uploaded without padding");

VertexLayout terrain_vertex_layout() {
    return VertexLayout{
        sizeof(TerrainVertex),
        {
            {0, VK_FORMAT_R32G32B32_SFLOAT, offsetof(TerrainVertex, position)},
            {1, VK_FORMAT_R8G8B8A8_UNORM, offsetof(TerrainVertex, color)},
            {2, VK_FORMAT_R32G32_SFLOAT, offsetof(TerrainVertex, texture_coordinate)},
            {3, VK_FORMAT_R16G16_UINT, offsetof(TerrainVertex, packed_light)},
        },
    };
}

}
