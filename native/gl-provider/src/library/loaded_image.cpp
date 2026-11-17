#include "library/loaded_image.hpp"

#include <algorithm>

namespace minemod::provider {

bool contains_address(const std::vector<ImageSegment>& segments, std::uintptr_t address) noexcept {
    return std::any_of(segments.begin(), segments.end(), [address](const ImageSegment& segment) {
        return segment.contains(address);
    });
}

}
