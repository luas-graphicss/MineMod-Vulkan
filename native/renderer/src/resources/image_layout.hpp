#pragma once

#include <vulkan/vulkan.h>

namespace minemod::renderer {

struct LayoutChange {
    VkImageLayout from;
    VkImageLayout to;
    VkPipelineStageFlags2 source_stage;
    VkAccessFlags2 source_access;
    VkPipelineStageFlags2 destination_stage;
    VkAccessFlags2 destination_access;
};

inline constexpr LayoutChange undefined_to_transfer_destination{
    VK_IMAGE_LAYOUT_UNDEFINED,
    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_NONE,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_TRANSFER_WRITE_BIT,
};

inline constexpr LayoutChange transfer_destination_to_sampled{
    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_TRANSFER_WRITE_BIT,
    VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
    VK_ACCESS_2_SHADER_READ_BIT,
};

inline constexpr LayoutChange transfer_destination_to_present{
    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
    VK_IMAGE_LAYOUT_PRESENT_SRC_KHR,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_TRANSFER_WRITE_BIT,
    VK_PIPELINE_STAGE_2_NONE,
    VK_ACCESS_2_NONE,
};

void change_layout(VkCommandBuffer commands, VkImage image, VkImageAspectFlags aspect, const LayoutChange& change);

}
