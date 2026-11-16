#pragma once

#include <optional>

#include <vulkan/vulkan.h>

#include "graph/image_use.hpp"
#include "resources/image_layout.hpp"

namespace minemod::renderer {

class ImageState {
public:
    explicit ImageState(const ImageUse& entry) noexcept;

    bool has_contents() const noexcept {
        return layout_ != VK_IMAGE_LAYOUT_UNDEFINED;
    }

    void discard_contents() noexcept {
        layout_ = VK_IMAGE_LAYOUT_UNDEFINED;
    }

    std::optional<LayoutChange> advance(const ImageUse& use) noexcept;

private:
    struct Scope {
        VkPipelineStageFlags2 stages = VK_PIPELINE_STAGE_2_NONE;
        VkAccessFlags2 access = VK_ACCESS_2_NONE;
    };

    bool is_visible_to(const ImageUse& use) const noexcept;
    void enter(const ImageUse& use) noexcept;

    VkImageLayout layout_;
    Scope last_write_;
    Scope synchronized_reads_;
};

}
