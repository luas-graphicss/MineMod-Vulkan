#include "frame/frames_in_flight.hpp"

#include <stdexcept>
#include <utility>

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

namespace {

void begin_commands(VkCommandBuffer commands) {
    const VkCommandBufferBeginInfo begin_info{
        .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO,
        .flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT,
    };
    require_success(vkBeginCommandBuffer(commands, &begin_info), "beginning a frame command buffer");
}

}

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
        for (std::size_t part = 0; part < slot.submitted_parts; ++part) {
            const VkFence submitted = slot.parts[part].submitted.get();
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
        recycle(slot);
    }

    require_success(vkResetCommandPool(device_.get(), slot.pool.get(), 0), "resetting a frame command pool");
    const VkCommandBuffer commands = slot.parts.front().commands;
    begin_commands(commands);
    is_recording_ = true;
    return Frame{next_frame_, commands};
}

FramesInFlight::Frame FramesInFlight::submit_part(const SubmitSemaphores& semaphores) {
    Slot& slot = recording_slot();
    enqueue_part(slot, semaphores);
    if (slot.submitted_parts == slot.parts.size()) {
        slot.parts.push_back(create_part(device_, slot.pool.get()));
    }
    const VkCommandBuffer commands = slot.parts[slot.submitted_parts].commands;
    begin_commands(commands);
    return Frame{next_frame_, commands};
}

void FramesInFlight::wait_for_submitted_parts() {
    wait_for_parts_of(recording_slot());
}

void FramesInFlight::submit(const SubmitSemaphores& semaphores) {
    Slot& slot = recording_slot();
    enqueue_part(slot, semaphores);
    slot.pending_frame = next_frame_;
    is_recording_ = false;
    ++next_frame_;
}

FramesInFlight::Slot& FramesInFlight::recording_slot() {
    if (!is_recording_) {
        throw std::logic_error("no frame is being recorded");
    }
    return next_slot();
}

void FramesInFlight::wait_for_parts_of(const Slot& slot) const {
    for (std::size_t part = 0; part < slot.submitted_parts; ++part) {
        const VkFence submitted = slot.parts[part].submitted.get();
        require_success(
            vkWaitForFences(device_.get(), 1, &submitted, VK_TRUE, UINT64_MAX), "waiting for a frame in flight");
    }
}

void FramesInFlight::recycle(Slot& slot) {
    wait_for_parts_of(slot);
    for (std::size_t part = 0; part < slot.submitted_parts; ++part) {
        const VkFence submitted = slot.parts[part].submitted.get();
        require_success(vkResetFences(device_.get(), 1, &submitted), "resetting a frame fence");
    }
    slot.submitted_parts = 0;
    completed_frame_ = std::exchange(slot.pending_frame, std::nullopt);
}

void FramesInFlight::enqueue_part(Slot& slot, const SubmitSemaphores& semaphores) {
    const Part& part = slot.parts[slot.submitted_parts];
    require_success(vkEndCommandBuffer(part.commands), "ending a frame command buffer");
    device_.enqueue(part.commands, semaphores, part.submitted.get());
    ++slot.submitted_parts;
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
    slot.parts.push_back(create_part(device, slot.pool.get()));
    return slot;
}

FramesInFlight::Part FramesInFlight::create_part(const Device& device, VkCommandPool pool) {
    Part part;
    const VkCommandBufferAllocateInfo allocation{
        .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,
        .commandPool = pool,
        .level = VK_COMMAND_BUFFER_LEVEL_PRIMARY,
        .commandBufferCount = 1,
    };
    require_success(
        vkAllocateCommandBuffers(device.get(), &allocation, &part.commands), "allocating a frame command buffer");

    const VkFenceCreateInfo fence_info{.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO};
    part.submitted = create_device_handle<VkFence>(
        device.get(), fence_info, vkCreateFence, vkDestroyFence, "creating a frame fence");
    return part;
}

}
