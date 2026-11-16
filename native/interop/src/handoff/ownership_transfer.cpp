#include "handoff/ownership_transfer.hpp"

#include <stdexcept>

namespace minemod::interop {
namespace {

constexpr VkAccessFlags2 any_access = VK_ACCESS_2_MEMORY_READ_BIT | VK_ACCESS_2_MEMORY_WRITE_BIT;

}

OwnershipTransfer OwnershipTransfer::release_from(std::uint32_t queue_family, const HandoffResources& resources) {
    return OwnershipTransfer(
        resources,
        Direction{
            queue_family,
            VK_QUEUE_FAMILY_EXTERNAL,
            VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,
            any_access,
            VK_PIPELINE_STAGE_2_NONE,
            VK_ACCESS_2_NONE,
        });
}

OwnershipTransfer OwnershipTransfer::acquire_for(std::uint32_t queue_family, const HandoffResources& resources) {
    return OwnershipTransfer(
        resources,
        Direction{
            VK_QUEUE_FAMILY_EXTERNAL,
            queue_family,
            VK_PIPELINE_STAGE_2_NONE,
            VK_ACCESS_2_NONE,
            VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,
            any_access,
        });
}

OwnershipTransfer::OwnershipTransfer(const HandoffResources& resources, const Direction& direction) {
    if (resources.images.empty() && resources.buffers.empty()) {
        throw std::invalid_argument("an ownership transfer needs at least one shared image or buffer");
    }

    images_.reserve(resources.images.size());
    for (const SharedImageUse& use : resources.images) {
        images_.push_back(VkImageMemoryBarrier2{
            .sType = VK_STRUCTURE_TYPE_IMAGE_MEMORY_BARRIER_2,
            .srcStageMask = direction.source_stage,
            .srcAccessMask = direction.source_access,
            .dstStageMask = direction.destination_stage,
            .dstAccessMask = direction.destination_access,
            .oldLayout = use.layout,
            .newLayout = use.layout,
            .srcQueueFamilyIndex = direction.from_family,
            .dstQueueFamilyIndex = direction.to_family,
            .image = use.image,
            .subresourceRange = {use.aspect, 0, VK_REMAINING_MIP_LEVELS, 0, VK_REMAINING_ARRAY_LAYERS},
        });
    }

    buffers_.reserve(resources.buffers.size());
    for (const VkBuffer buffer : resources.buffers) {
        buffers_.push_back(VkBufferMemoryBarrier2{
            .sType = VK_STRUCTURE_TYPE_BUFFER_MEMORY_BARRIER_2,
            .srcStageMask = direction.source_stage,
            .srcAccessMask = direction.source_access,
            .dstStageMask = direction.destination_stage,
            .dstAccessMask = direction.destination_access,
            .srcQueueFamilyIndex = direction.from_family,
            .dstQueueFamilyIndex = direction.to_family,
            .buffer = buffer,
            .offset = 0,
            .size = VK_WHOLE_SIZE,
        });
    }
}

void OwnershipTransfer::record(VkCommandBuffer commands) const {
    const VkDependencyInfo dependency{
        .sType = VK_STRUCTURE_TYPE_DEPENDENCY_INFO,
        .bufferMemoryBarrierCount = static_cast<std::uint32_t>(buffers_.size()),
        .pBufferMemoryBarriers = buffers_.data(),
        .imageMemoryBarrierCount = static_cast<std::uint32_t>(images_.size()),
        .pImageMemoryBarriers = images_.data(),
    };
    vkCmdPipelineBarrier2(commands, &dependency);
}

}
