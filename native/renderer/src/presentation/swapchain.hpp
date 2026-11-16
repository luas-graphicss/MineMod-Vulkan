#pragma once

#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "resources/image.hpp"

namespace minemod::renderer {

class Swapchain {
public:
    Swapchain(const Device& device, VkSurfaceKHR surface, VkExtent2D desired_extent);
    ~Swapchain();

    Swapchain(const Swapchain&) = delete;
    Swapchain& operator=(const Swapchain&) = delete;

    VkFormat format() const noexcept {
        return format_;
    }

    void present(const Device& device, const Image& transfer_source);

private:
    VkDevice device_;
    VkFormat format_;
    VkExtent2D extent_;
    DeviceHandle<VkSwapchainKHR> swapchain_;
    std::vector<VkImage> images_;
    DeviceHandle<VkSemaphore> image_acquired_;
    DeviceHandle<VkSemaphore> frame_copied_;
};

}
