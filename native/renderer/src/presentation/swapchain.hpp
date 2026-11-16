#pragma once

#include <cstdint>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"

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

    VkExtent2D extent() const noexcept {
        return extent_;
    }

    VkImage image(std::uint32_t index) const {
        return images_.at(index);
    }

    VkResult acquire(VkSemaphore signaled_on_acquisition, std::uint32_t& image_index) const;
    VkResult present(VkQueue queue, VkSemaphore awaited, std::uint32_t image_index) const;

private:
    VkDevice device_;
    VkFormat format_;
    VkExtent2D extent_;
    DeviceHandle<VkSwapchainKHR> swapchain_;
    std::vector<VkImage> images_;
};

}
