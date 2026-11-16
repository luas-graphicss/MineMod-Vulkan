#pragma once

#include <vulkan/vulkan.h>

#include "environment/fog.hpp"
#include "math/matrix.hpp"
#include "math/vector.hpp"

namespace minemod::renderer {

struct TerrainFrame {
    VkExtent2D extent;
    Matrix4 view_projection;
    WorldPosition camera;
    Fog fog;
    VkDescriptorSet textures;
};

}
