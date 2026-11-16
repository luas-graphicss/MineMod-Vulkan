#pragma once

#include <vulkan/vulkan.h>

#include "graph/render_graph.hpp"

namespace minemod::renderer::test_support {

inline constexpr VkExtent2D target_extent{8, 8};

inline constexpr ImageDescription color_target{
    target_extent,
    VK_FORMAT_R8G8B8A8_UNORM,
    VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
    VK_IMAGE_ASPECT_COLOR_BIT,
};

inline constexpr ImageDescription depth_target{
    target_extent,
    VK_FORMAT_D32_SFLOAT,
    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
    VK_IMAGE_ASPECT_DEPTH_BIT,
};

inline void record_nothing(VkCommandBuffer, const GraphImages&) {}

}
