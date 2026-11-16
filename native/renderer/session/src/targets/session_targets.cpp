#include "targets/session_targets.hpp"

#include <utility>

namespace minemod::scene {

namespace {

FrameTarget color_target(const renderer::Image& image) noexcept {
    return FrameTarget{image.get(), image.view(), VK_IMAGE_ASPECT_COLOR_BIT};
}

FrameTarget shared_target(const AttachmentImage& image) noexcept {
    return FrameTarget{image.image, image.view, image.aspect};
}

}

SessionTargets::SessionTargets(const renderer::Device& device, VkExtent2D extent)
    : owned_(std::in_place_type<renderer::FrameTargets>, device, extent) {}

SessionTargets::SessionTargets(
    const renderer::Device& device, VkExtent2D extent, std::unique_ptr<SharedTargets> shared)
    : owned_(
          std::in_place_type<SharedWithProvider>,
          std::move(shared),
          renderer::create_frame_scratch(device, extent)) {}

FrameTarget SessionTargets::color() const noexcept {
    if (const SharedTargets* attachments = shared()) {
        return shared_target(attachments->attachment(SharedAttachment::color));
    }
    return color_target(exclusive().color());
}

FrameTarget SessionTargets::depth() const noexcept {
    if (const SharedTargets* attachments = shared()) {
        return shared_target(attachments->attachment(SharedAttachment::depth_stencil));
    }
    const renderer::Image& image = exclusive().depth();
    return FrameTarget{image.get(), image.view(), VK_IMAGE_ASPECT_DEPTH_BIT};
}

FrameTarget SessionTargets::scratch() const noexcept {
    if (const auto* with_provider = std::get_if<SharedWithProvider>(&owned_)) {
        return color_target(with_provider->scratch);
    }
    return color_target(exclusive().scratch());
}

VkFormat SessionTargets::depth_format() const noexcept {
    if (const SharedTargets* attachments = shared()) {
        return attachments->attachment(SharedAttachment::depth_stencil).format;
    }
    return renderer::frame_depth_format;
}

const SharedTargets* SessionTargets::shared() const noexcept {
    const auto* with_provider = std::get_if<SharedWithProvider>(&owned_);
    return with_provider == nullptr ? nullptr : with_provider->attachments.get();
}

const renderer::FrameTargets& SessionTargets::exclusive() const noexcept {
    return *std::get_if<renderer::FrameTargets>(&owned_);
}

}
