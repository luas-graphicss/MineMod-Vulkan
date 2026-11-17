#pragma once

#include <cstdint>
#include <vector>

#include <vulkan/vulkan.h>

namespace minemod::interop {

struct SharedImageUse {
    VkImage image;
    VkImageAspectFlags aspect;
    VkImageLayout layout;
};

struct HandoffResources {
    std::vector<SharedImageUse> images;
    std::vector<VkBuffer> buffers;
};

class OwnershipTransfer {
public:
    static OwnershipTransfer release_from(std::uint32_t queue_family, const HandoffResources& resources);
    static OwnershipTransfer acquire_for(std::uint32_t queue_family, const HandoffResources& resources);

    void record(VkCommandBuffer commands) const;

private:
    struct Direction {
        std::uint32_t from_family;
        std::uint32_t to_family;
        VkPipelineStageFlags2 source_stage;
        VkAccessFlags2 source_access;
        VkPipelineStageFlags2 destination_stage;
        VkAccessFlags2 destination_access;
    };

    OwnershipTransfer(const HandoffResources& resources, const Direction& direction);

    std::vector<VkImageMemoryBarrier2> images_;
    std::vector<VkBufferMemoryBarrier2> buffers_;
};

}
