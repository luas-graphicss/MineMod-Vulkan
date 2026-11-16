#include "frame/frames_in_flight.hpp"

#include <stdexcept>
#include <utility>

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

FramesInFlight::FramesInFlight(const Device& device, std::size_t frame_count) : device_(device) {
    if (frame_count == 0) {
        throw std::invalid_argument("at least one frame must be in flight");
    }
    slots_.reserve(frame_count);
    for (std::size_t slot = 0; slot < frame_count; ++slot) {
        slots_.push_back(create_slot(device));
    }
}

FramesInFlight::~FramesInFlight() {
    for (const Slot& slot : slots_) {
        if (slot.pending_frame) {
            const VkFence submitted = slot.submitted.get();
            vkWaitForFences(device_.get(), 1, &submitted, VK_TRUE, UINT64_MAX);
        }
    }
}

FramesInFlight::Frame FramesInFlight::begin() {
    if (is_recording_) {
        throw std::logic_error("the frame being recorded was not submitted");
    }
    Slot& slot = next_slot();
    if (slot.pending_frame) {
        const VkFence submitted = slot.submitted.get();
        require_success(
            vkWaitForFences(device_.get(), 1, &submitted, VK_TRUE, UINT64_MAX), "waiting for a frame in flight");
        require_success(vkResetFences(device_.get(), 1, &submitted), "resetting a frame fence");
        completed_frame_ = std::exchange(slot.pending_frame, std::nullopt);
    }

    require_success(vkResetCommandPool(device_.get(), slot.pool.get(), 0), "resetting a frame command pool");
    const VkCommandBufferBeginInfo begin_info{
        .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO,
        .flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT,
    };
    require_success(vkBeginCommandBuffer(slot.commands, &begin_info), "beginning a frame command buffer");
    is_recording_ = true;
    return Frame{next_frame_, slot.commands};
}

void FramesInFlight::submit(const SubmitSemaphores& semaphores) {
    if (!is_recording_) {
        throw std::logic_error("no frame is being recorded");
    }
    Slot& slot = next_slot();
    require_success(vkEndCommandBuffer(slot.commands), "ending a frame command buffer");
    device_.enqueue(slot.commands, semaphores, slot.submitted.get());
    slot.pending_frame = next_frame_;
    is_recording_ = false;
    ++next_frame_;
}

FramesInFlight::Slot FramesInFlight::create_slot(const Device& device) {
    Slot slot;
    const VkCommandPoolCreateInfo pool_info{
        .sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,
        .flags = VK_COMMAND_POOL_CREATE_TRANSIENT_BIT,
        .queueFamilyIndex = device.queue_family(),
    };
    slot.pool = create_device_handle<VkCommandPool>(
        device.get(), pool_info, vkCreateCommandPool, vkDestroyCommandPool, "creating a frame command pool");

    const VkCommandBufferAllocateInfo allocation{
        .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,
        .commandPool = slot.pool.get(),
        .level = VK_COMMAND_BUFFER_LEVEL_PRIMARY,
        .commandBufferCount = 1,
    };
    require_success(
        vkAllocateCommandBuffers(device.get(), &allocation, &slot.commands), "allocating a frame command buffer");

    const VkFenceCreateInfo fence_info{.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO};
    slot.submitted = create_device_handle<VkFence>(
        device.get(), fence_info, vkCreateFence, vkDestroyFence, "creating a frame fence");
    return slot;
}

}
