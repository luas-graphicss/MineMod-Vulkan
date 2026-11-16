#include "upload/upload_arena.hpp"

namespace minemod::renderer {

UploadArena::UploadArena(const Device& device, VkDeviceSize capacity, VkBufferUsageFlags usage)
    : staging_(device, capacity, usage, host_memory), layout_(capacity) {}

std::optional<ArenaRegion> UploadArena::stage(std::span<const std::byte> bytes, VkDeviceSize alignment) {
    const std::optional<ArenaRegion> region = layout_.claim(bytes.size(), alignment);
    if (region) {
        staging_.write(bytes, region->offset);
    }
    return region;
}

}
