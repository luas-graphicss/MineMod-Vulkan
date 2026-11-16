#include "graph/image_state.hpp"

namespace minemod::renderer {

ImageState::ImageState(const ImageUse& entry) noexcept : layout_(entry.layout) {
    enter(entry);
}

std::optional<LayoutChange> ImageState::advance(const ImageUse& use) noexcept {
    const bool is_write = writes_image(use);
    const bool changes_layout = use.layout != layout_;
    if (!is_write && !changes_layout && is_visible_to(use)) {
        return std::nullopt;
    }

    const LayoutChange change{
        layout_,
        use.layout,
        last_write_.stages | synchronized_reads_.stages,
        last_write_.access,
        use.stages,
        use.access,
    };

    layout_ = use.layout;
    if (is_write || changes_layout) {
        synchronized_reads_ = Scope{};
    }
    enter(use);
    return change;
}

bool ImageState::is_visible_to(const ImageUse& use) const noexcept {
    return (use.stages & ~synchronized_reads_.stages) == 0 && (use.access & ~synchronized_reads_.access) == 0;
}

void ImageState::enter(const ImageUse& use) noexcept {
    if (writes_image(use)) {
        last_write_ = Scope{use.stages, use.access};
        return;
    }
    synchronized_reads_.stages |= use.stages;
    synchronized_reads_.access |= use.access;
}

}
