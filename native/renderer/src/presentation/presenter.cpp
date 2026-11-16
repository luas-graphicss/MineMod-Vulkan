#include "presentation/presenter.hpp"

#include <stdexcept>
#include <utility>

#include "presentation/binary_semaphore.hpp"
#include "resources/image_layout.hpp"

namespace minemod::renderer {

namespace {

VkOffset3D far_corner(VkExtent2D extent) noexcept {
    return VkOffset3D{static_cast<std::int32_t>(extent.width), static_cast<std::int32_t>(extent.height), 1};
}

void blit_frame(VkCommandBuffer commands, const Image& source, const PresentationTarget& target) {
    const VkImageBlit2 region{
        .sType = VK_STRUCTURE_TYPE_IMAGE_BLIT_2,
        .srcSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
        .srcOffsets = {VkOffset3D{0, 0, 0}, far_corner(source.extent())},
        .dstSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
        .dstOffsets = {VkOffset3D{0, 0, 0}, far_corner(target.extent)},
    };
    const VkBlitImageInfo2 blit{
        .sType = VK_STRUCTURE_TYPE_BLIT_IMAGE_INFO_2,
        .srcImage = source.get(),
        .srcImageLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
        .dstImage = target.image,
        .dstImageLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
        .regionCount = 1,
        .pRegions = &region,
        .filter = VK_FILTER_NEAREST,
    };
    vkCmdBlitImage2(commands, &blit);
}

}

Presenter::Presenter(const Device& device, VkSurfaceKHR surface, VkExtent2D surface_extent, std::size_t frame_slots)
    : device_(device), surface_(surface), surface_extent_(surface_extent), lifecycle_(surface_extent) {
    if (frame_slots == 0) {
        throw std::invalid_argument("a presenter needs at least one frame slot");
    }
    acquired_.reserve(frame_slots);
    for (std::size_t slot = 0; slot < frame_slots; ++slot) {
        acquired_.push_back(create_binary_semaphore(device.get()));
    }
    if (lifecycle_.state() == FrameState::awaiting_swapchain) {
        replace_swapchain();
    }
}

VkFormat Presenter::format() const {
    if (!swapchain_) {
        throw std::logic_error("the surface has had no area yet, so it has no presentation format");
    }
    return swapchain_->format();
}

void Presenter::resize(VkExtent2D surface_extent) {
    surface_extent_ = surface_extent;
    lifecycle_.resize(surface_extent);
}

void Presenter::set_pacing(PresentPacing pacing) noexcept {
    if (pacing == pacing_) {
        return;
    }
    pacing_ = pacing;
    lifecycle_.swapchain_outdated();
}

std::optional<PresentationTarget> Presenter::acquire(std::size_t frame_slot) {
    if (lifecycle_.state() == FrameState::awaiting_swapchain) {
        replace_swapchain();
    }
    if (lifecycle_.state() != FrameState::running) {
        return std::nullopt;
    }
    const VkSemaphore image_acquired = acquired_.at(frame_slot).get();
    std::uint32_t image_index = 0;
    const VkResult acquired = swapchain_->acquire(image_acquired, image_index);
    if (lifecycle_.acquired(acquired) == AcquiredFrame::skip) {
        return std::nullopt;
    }
    return PresentationTarget{
        swapchain_->image(image_index),
        swapchain_->extent(),
        SubmitSemaphores{
            .wait = image_acquired,
            .wait_stage = VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
            .signal = swapchain_->copied_semaphore(image_index),
        },
        image_index,
    };
}

void Presenter::record_copy(
    VkCommandBuffer commands, const Image& transfer_source, const PresentationTarget& target) const {
    change_layout(commands, target.image, VK_IMAGE_ASPECT_COLOR_BIT, undefined_to_transfer_destination);
    blit_frame(commands, transfer_source, target);
    change_layout(commands, target.image, VK_IMAGE_ASPECT_COLOR_BIT, transfer_destination_to_present);
}

void Presenter::present(const PresentationTarget& target) {
    lifecycle_.presented(swapchain_->present(device_.queue(), target.image_index));
}

void Presenter::replace_swapchain() {
    swapchain_ = Swapchain::create(device_, surface_, surface_extent_, pacing_, std::move(swapchain_));
    if (swapchain_) {
        lifecycle_.swapchain_created();
    }
}

}
