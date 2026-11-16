#pragma once

#include <memory>
#include <variant>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "frame/frame_targets.hpp"
#include "resources/image.hpp"
#include "sharing/provider_sharing.hpp"

namespace minemod::scene {

struct FrameTarget {
    VkImage image;
    VkImageView view;
    VkImageAspectFlags aspect;
};

class SessionTargets {
public:
    SessionTargets(const renderer::Device& device, VkExtent2D extent);
    SessionTargets(const renderer::Device& device, VkExtent2D extent, std::unique_ptr<SharedTargets> shared);

    FrameTarget color() const noexcept;
    FrameTarget depth() const noexcept;
    FrameTarget scratch() const noexcept;
    VkFormat depth_format() const noexcept;
    const SharedTargets* shared() const noexcept;

private:
    struct SharedWithProvider {
        std::unique_ptr<SharedTargets> attachments;
        renderer::Image scratch;
    };

    const renderer::FrameTargets& exclusive() const noexcept;

    std::variant<renderer::FrameTargets, SharedWithProvider> owned_;
};

}
