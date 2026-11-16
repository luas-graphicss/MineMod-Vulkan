#pragma once

#include <array>
#include <optional>

#include <vulkan/vulkan.h>

#include "graph/graph_images.hpp"

namespace minemod::renderer {

struct FrameAttachments {
    BoundImage color;
    BoundImage depth;
};

void begin_frame_rendering(
    VkCommandBuffer commands, const FrameAttachments& attachments, const std::optional<std::array<float, 4>>& clear_color);

void clear_frame_depth(VkCommandBuffer commands, VkExtent2D extent);

}
