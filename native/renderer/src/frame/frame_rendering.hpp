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

struct DepthOperations {
    VkAttachmentLoadOp load = VK_ATTACHMENT_LOAD_OP_CLEAR;
    VkAttachmentStoreOp store = VK_ATTACHMENT_STORE_OP_DONT_CARE;
};

void begin_frame_rendering(
    VkCommandBuffer commands,
    const FrameAttachments& attachments,
    const std::optional<std::array<float, 4>>& clear_color,
    const DepthOperations& depth_operations = {});

void clear_frame_depth(VkCommandBuffer commands, VkExtent2D extent);

}
