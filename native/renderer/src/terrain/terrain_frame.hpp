#pragma once

#include <optional>
#include <span>

#include <vulkan/vulkan.h>

#include "environment/fog.hpp"
#include "math/matrix.hpp"
#include "math/vector.hpp"
#include "work/job_epochs.hpp"

namespace minemod::renderer {

struct TerrainFrame {
    VkExtent2D extent;
    Matrix4 view_projection;
    WorldPosition camera;
    Fog fog;
    VkDescriptorSet textures;
    std::optional<std::span<const SectionPosition>> reachable_sections{};
};

}
