#pragma once

#include <optional>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

struct ArenaRegion {
    VkDeviceSize offset;
    VkDeviceSize size;

    friend bool operator==(const ArenaRegion&, const ArenaRegion&) = default;
};

class ArenaLayout {
public:
    explicit ArenaLayout(VkDeviceSize capacity) noexcept : capacity_(capacity) {}

    std::optional<ArenaRegion> claim(VkDeviceSize size, VkDeviceSize alignment);

    void reset() noexcept {
        used_ = 0;
    }

    VkDeviceSize used() const noexcept {
        return used_;
    }

    VkDeviceSize capacity() const noexcept {
        return capacity_;
    }

private:
    VkDeviceSize capacity_;
    VkDeviceSize used_ = 0;
};

}
