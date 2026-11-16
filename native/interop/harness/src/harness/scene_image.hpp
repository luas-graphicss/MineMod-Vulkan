#pragma once

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "frame/frame_readback.hpp"
#include "handoff/ownership_transfer.hpp"
#include "memory/shared_image.hpp"

namespace minemod::interop::harness {

enum class SceneImageKind { color, depth_stencil };

struct SceneImageDescription {
    SceneImageKind kind;
    VkExtent2D extent;
    VkFormat format;
};

VkImageUsageFlags scene_image_usage(SceneImageKind kind) noexcept;

class SceneImage {
public:
    SceneImage(const renderer::Device& device, const SceneImageDescription& description);

    const SharedImage& shared() const noexcept {
        return image_;
    }

    SharedImageUse handoff_use() const noexcept;

    void record_clear(VkCommandBuffer commands, const VkRect2D& area, const VkClearValue& value) const;
    void clear(const VkRect2D& area, const VkClearValue& value) const;
    renderer::FramePixels read_color() const;

private:
    const renderer::Device& device_;
    SceneImageDescription description_;
    SharedImage image_;
    renderer::DeviceHandle<VkImageView> view_;
};

}
