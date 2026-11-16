#include "presentation/presenter.hpp"

#include <stdexcept>

#include "resources/image_layout.hpp"

namespace minemod::renderer {

namespace {

DeviceHandle<VkSemaphore> create_semaphore(VkDevice device) {
    const VkSemaphoreCreateInfo create_info{.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
    return create_device_handle<VkSemaphore>(
        device, create_info, vkCreateSemaphore, vkDestroySemaphore, "creating a presentation semaphore");
}

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
    slots_.reserve(frame_slots);
    for (std::size_t slot = 0; slot < frame_slots; ++slot) {
        slots_.push_back({create_semaphore(device.get()), create_semaphore(device.get())});
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

std::optional<PresentationTarget> Presenter::acquire(std::size_t frame_slot) {
    if (lifecycle_.state() == FrameState::awaiting_swapchain) {
        replace_swapchain();
    }
    if (lifecycle_.state() != FrameState::running) {
        return std::nullopt;
    }
    const SlotSemaphores& slot = slots_.at(frame_slot);
    std::uint32_t image_index = 0;
    const VkResult acquired = swapchain_->acquire(slot.image_acquired.get(), image_index);
    if (lifecycle_.acquired(acquired) == AcquiredFrame::skip) {
        return std::nullopt;
    }
    return PresentationTarget{
        swapchain_->image(image_index),
        swapchain_->extent(),
        SubmitSemaphores{
            .wait = slot.image_acquired.get(),
            .wait_stage = VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
            .signal = slot.frame_copied.get(),
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
    lifecycle_.presented(swapchain_->present(device_.queue(), target.semaphores.signal, target.image_index));
}

void Presenter::replace_swapchain() {
    swapchain_.reset();
    swapchain_.emplace(device_, surface_, surface_extent_);
    lifecycle_.swapchain_created();
}

}
