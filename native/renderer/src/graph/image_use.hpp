#pragma once

#include <vulkan/vulkan.h>

namespace minemod::renderer {

struct ImageUse {
    VkImageLayout layout;
    VkPipelineStageFlags2 stages;
    VkAccessFlags2 access;

    friend bool operator==(const ImageUse&, const ImageUse&) = default;
};

namespace uses {

inline constexpr ImageUse untouched{VK_IMAGE_LAYOUT_UNDEFINED, VK_PIPELINE_STAGE_2_NONE, VK_ACCESS_2_NONE};

inline constexpr ImageUse color_attachment{
    VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
    VK_PIPELINE_STAGE_2_COLOR_ATTACHMENT_OUTPUT_BIT,
    VK_ACCESS_2_COLOR_ATTACHMENT_READ_BIT | VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT,
};

inline constexpr ImageUse depth_attachment{
    VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
    VK_PIPELINE_STAGE_2_EARLY_FRAGMENT_TESTS_BIT | VK_PIPELINE_STAGE_2_LATE_FRAGMENT_TESTS_BIT,
    VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_READ_BIT | VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT,
};

inline constexpr ImageUse sampled_by_fragment_shader{
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
    VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
    VK_ACCESS_2_SHADER_SAMPLED_READ_BIT,
};

inline constexpr ImageUse sampled_by_shaders{
    VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
    VK_PIPELINE_STAGE_2_VERTEX_SHADER_BIT | VK_PIPELINE_STAGE_2_FRAGMENT_SHADER_BIT,
    VK_ACCESS_2_SHADER_SAMPLED_READ_BIT,
};

inline constexpr ImageUse transfer_source{
    VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_TRANSFER_READ_BIT,
};

inline constexpr ImageUse transfer_destination{
    VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_TRANSFER_WRITE_BIT,
};

}

inline constexpr VkAccessFlags2 image_write_accesses = VK_ACCESS_2_SHADER_WRITE_BIT
    | VK_ACCESS_2_SHADER_STORAGE_WRITE_BIT | VK_ACCESS_2_COLOR_ATTACHMENT_WRITE_BIT
    | VK_ACCESS_2_DEPTH_STENCIL_ATTACHMENT_WRITE_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT | VK_ACCESS_2_HOST_WRITE_BIT
    | VK_ACCESS_2_MEMORY_WRITE_BIT;

constexpr bool writes_image(const ImageUse& use) noexcept {
    return (use.access & image_write_accesses) != 0;
}

}
