#include "upload/upload_arena.hpp"

namespace minemod::renderer {

UploadArena::UploadArena(const Device& device, VkDeviceSize capacity)
    : staging_(device, capacity, VK_BUFFER_USAGE_TRANSFER_SRC_BIT, host_memory), layout_(capacity) {}

std::optional<ArenaRegion> UploadArena::stage(std::span<const std::byte> bytes, VkDeviceSize alignment) {
    const std::optional<ArenaRegion> region = layout_.claim(bytes.size(), alignment);
    if (region) {
        staging_.write(bytes, region->offset);
    }
    return region;
}

}
