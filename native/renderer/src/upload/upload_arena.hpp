#pragma once

#include <cstddef>
#include <optional>
#include <span>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "resources/buffer.hpp"
#include "upload/arena_layout.hpp"

namespace minemod::renderer {

class UploadArena {
public:
    UploadArena(
        const Device& device, VkDeviceSize capacity, VkBufferUsageFlags usage = VK_BUFFER_USAGE_TRANSFER_SRC_BIT);

    std::optional<ArenaRegion> stage(std::span<const std::byte> bytes, VkDeviceSize alignment);

    void reset() noexcept {
        layout_.reset();
    }

    VkBuffer buffer() const noexcept {
        return staging_.get();
    }

    VkDeviceSize used() const noexcept {
        return layout_.used();
    }

private:
    Buffer staging_;
    ArenaLayout layout_;
};

}
