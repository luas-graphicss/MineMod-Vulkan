#include "interface/clip_rectangle.hpp"

#include <algorithm>
#include <limits>

namespace minemod::renderer {
namespace {

constexpr std::int64_t largest_offset = std::numeric_limits<std::int32_t>::max();

struct PixelRange {
    std::int64_t first;
    std::int64_t past_last;
};

constexpr PixelRange range_inside(std::int64_t first, std::int64_t length, std::int64_t limit) noexcept {
    const std::int64_t clamped_first = std::clamp<std::int64_t>(first, 0, limit);
    return PixelRange{clamped_first, std::clamp<std::int64_t>(first + length, clamped_first, limit)};
}

}

VkRect2D clip_rectangle(const LowerLeftRectangle& rectangle, VkExtent2D frame) noexcept {
    const std::int64_t frame_width = std::min<std::int64_t>(frame.width, largest_offset);
    const std::int64_t frame_height = std::min<std::int64_t>(frame.height, largest_offset);
    const std::int64_t width = std::max<std::int64_t>(rectangle.width, 0);
    const std::int64_t height = std::max<std::int64_t>(rectangle.height, 0);

    const PixelRange columns = range_inside(rectangle.left, width, frame_width);
    const PixelRange rows = range_inside(frame_height - rectangle.bottom - height, height, frame_height);
    return VkRect2D{
        {static_cast<std::int32_t>(columns.first), static_cast<std::int32_t>(rows.first)},
        {
            static_cast<std::uint32_t>(columns.past_last - columns.first),
            static_cast<std::uint32_t>(rows.past_last - rows.first),
        },
    };
}

}
