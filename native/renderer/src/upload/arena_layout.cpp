#include "upload/arena_layout.hpp"

#include <bit>
#include <stdexcept>

namespace minemod::renderer {

std::optional<ArenaRegion> ArenaLayout::claim(VkDeviceSize size, VkDeviceSize alignment) {
    if (size == 0) {
        throw std::invalid_argument("an arena region must hold at least one byte");
    }
    if (!std::has_single_bit(alignment)) {
        throw std::invalid_argument("an arena alignment must be a power of two");
    }

    const VkDeviceSize padding = (alignment - used_ % alignment) % alignment;
    const VkDeviceSize available = capacity_ - used_;
    if (padding > available || size > available - padding) {
        return std::nullopt;
    }

    const ArenaRegion region{used_ + padding, size};
    used_ = region.offset + region.size;
    return region;
}

}
