#include "resources/image_layout.hpp"

namespace minemod::renderer {

void change_layout(VkCommandBuffer commands, VkImage image, VkImageAspectFlags aspect, const LayoutChange& change) {
    const VkImageMemoryBarrier2 barrier{
        .sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2,
        .srcStageMask = change.source_stage,
        .srcAccessMask = change.source_access,
        .dstStageMask = change.destination_stage,
        .dstAccessMask = change.destination_access,
        .oldLayout = change.from,
        .newLayout = change.to,
        .srcQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
        .dstQueueFamilyIndex = VK_QUEUE_FAMILY_IGNORED,
        .image = image,
        .subresourceRange = {aspect, 0, VK_REMAINING_MIP_LEVELS, 0, 1},
    };
    const VkDependencyInfo dependency{
        .sType = VK_STRUCTURE_TYPE_DEPENDENCY_INFO,
        .imageMemoryBarrierCount = 1,
        .pImageMemoryBarriers = &barrier,
    };
    vkCmdPipelineBarrier2(commands, &dependency);
}

}
