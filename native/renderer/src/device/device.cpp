#include "device/device.hpp"

#include <cstdint>

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

namespace {

class OneTimeCommands {
public:
    OneTimeCommands(VkDevice device, VkCommandPool pool) : device_(device), pool_(pool) {
        const VkCommandBufferAllocateInfo allocation{
            .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_ALLOCATE_INFO,
            .commandPool = pool,
            .level = VK_COMMAND_BUFFER_LEVEL_PRIMARY,
            .commandBufferCount = 1,
        };
        require_success(vkAllocateCommandBuffers(device, &allocation, &commands_), "allocating a command buffer");
    }

    OneTimeCommands(const OneTimeCommands&) = delete;
    OneTimeCommands& operator=(const OneTimeCommands&) = delete;

    ~OneTimeCommands() {
        vkFreeCommandBuffers(device_, pool_, 1, &commands_);
    }

    VkCommandBuffer get() const noexcept {
        return commands_;
    }

private:
    VkDevice device_;
    VkCommandPool pool_;
    VkCommandBuffer commands_ = VK_NULL_HANDLE;
};

}

Device::LogicalDevice::~LogicalDevice() {
    if (handle != VK_NULL_HANDLE) {
        vkDestroyDevice(handle, nullptr);
    }
}

Device::Device(const SelectedDevice& selected, std::span<const char* const> extensions)
    : physical_(selected.handle), queue_family_(selected.queue_family) {
    const float queue_priority = 1.0f;
    const VkDeviceQueueCreateInfo queue_info{
        .sType = VK_STRUCTURE_TYPE_DEVICE_QUEUE_CREATE_INFO,
        .queueFamilyIndex = selected.queue_family,
        .queueCount = 1,
        .pQueuePriorities = &queue_priority,
    };
    VkPhysicalDeviceVulkan13Features core_features{
        .sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES,
        .synchronization2 = VK_TRUE,
        .dynamicRendering = VK_TRUE,
    };
    const VkDeviceCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_DEVICE_CREATE_INFO,
        .pNext = &core_features,
        .queueCreateInfoCount = 1,
        .pQueueCreateInfos = &queue_info,
        .enabledExtensionCount = static_cast<std::uint32_t>(extensions.size()),
        .ppEnabledExtensionNames = extensions.data(),
    };
    require_success(vkCreateDevice(physical_, &create_info, nullptr, &logical_.handle), "creating the Vulkan device");
    vkGetDeviceQueue(logical_.handle, selected.queue_family, 0, &queue_);

    const VkCommandPoolCreateInfo pool_info{
        .sType = VK_STRUCTURE_TYPE_COMMAND_POOL_CREATE_INFO,
        .flags = VK_COMMAND_POOL_CREATE_TRANSIENT_BIT,
        .queueFamilyIndex = selected.queue_family,
    };
    command_pool_ = create_device_handle<VkCommandPool>(
        logical_.handle, pool_info, vkCreateCommandPool, vkDestroyCommandPool, "creating the command pool");
}

void Device::submit(const std::function<void(VkCommandBuffer)>& record, const SubmitSemaphores& semaphores) const {
    const VkDevice device = logical_.handle;
    const OneTimeCommands commands(device, command_pool_.get());

    const VkCommandBufferBeginInfo begin{
        .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_BEGIN_INFO,
        .flags = VK_COMMAND_BUFFER_USAGE_ONE_TIME_SUBMIT_BIT,
    };
    require_success(vkBeginCommandBuffer(commands.get(), &begin), "beginning a command buffer");
    record(commands.get());
    require_success(vkEndCommandBuffer(commands.get()), "ending a command buffer");

    const VkFenceCreateInfo fence_info{.sType = VK_STRUCTURE_TYPE_FENCE_CREATE_INFO};
    const auto completed = create_device_handle<VkFence>(
        device, fence_info, vkCreateFence, vkDestroyFence, "creating a submission fence");

    enqueue(commands.get(), semaphores, completed.get());

    const VkFence fence = completed.get();
    require_success(vkWaitForFences(device, 1, &fence, VK_TRUE, UINT64_MAX), "waiting for the submitted commands");
}

void Device::enqueue(VkCommandBuffer commands, const SubmitSemaphores& semaphores, VkFence completed) const {
    const VkSemaphoreSubmitInfo wait{
        .sType = VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO,
        .semaphore = semaphores.wait,
        .stageMask = semaphores.wait_stage,
    };
    const VkSemaphoreSubmitInfo signal{
        .sType = VK_STRUCTURE_TYPE_SEMAPHORE_SUBMIT_INFO,
        .semaphore = semaphores.signal,
        .stageMask = VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,
    };
    const VkCommandBufferSubmitInfo command_info{
        .sType = VK_STRUCTURE_TYPE_COMMAND_BUFFER_SUBMIT_INFO,
        .commandBuffer = commands,
    };
    const VkSubmitInfo2 submission{
        .sType = VK_STRUCTURE_TYPE_SUBMIT_INFO_2,
        .waitSemaphoreInfoCount = semaphores.wait == VK_NULL_HANDLE ? 0u : 1u,
        .pWaitSemaphoreInfos = &wait,
        .commandBufferInfoCount = 1,
        .pCommandBufferInfos = &command_info,
        .signalSemaphoreInfoCount = semaphores.signal == VK_NULL_HANDLE ? 0u : 1u,
        .pSignalSemaphoreInfos = &signal,
    };
    require_success(vkQueueSubmit2(queue_, 1, &submission, completed), "submitting commands");
}

}
