#pragma once

#include <cstdint>
#include <memory>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "presentation/present_pacing.hpp"

namespace minemod::renderer {

class Swapchain {
public:
    static std::unique_ptr<Swapchain> create(
        const Device& device,
        VkSurfaceKHR surface,
        VkExtent2D desired_extent,
        PresentPacing pacing,
        std::unique_ptr<Swapchain> replaced);
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

    VkSemaphore copied_semaphore(std::uint32_t image_index) const {
        return copied_.at(image_index).get();
    }

    VkResult acquire(VkSemaphore signaled_on_acquisition, std::uint32_t& image_index) const;
    VkResult present(VkQueue queue, std::uint32_t image_index) const;

private:
    Swapchain(VkDevice device, const VkSwapchainCreateInfoKHR& create_info);

    VkDevice device_;
    VkFormat format_;
    VkExtent2D extent_;
    DeviceHandle<VkSwapchainKHR> swapchain_;
    std::vector<VkImage> images_;
    std::vector<DeviceHandle<VkSemaphore>> copied_;
};

}
