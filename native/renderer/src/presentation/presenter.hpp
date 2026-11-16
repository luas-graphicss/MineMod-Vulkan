#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "presentation/frame_lifecycle.hpp"
#include "presentation/swapchain.hpp"
#include "resources/image.hpp"

namespace minemod::renderer {

struct PresentationTarget {
    VkImage image;
    VkExtent2D extent;
    SubmitSemaphores semaphores;
    std::uint32_t image_index;
};

class Presenter {
public:
    Presenter(const Device& device, VkSurfaceKHR surface, VkExtent2D surface_extent, std::size_t frame_slots);

    FrameState state() const noexcept {
        return lifecycle_.state();
    }

    VkFormat format() const;

    void resize(VkExtent2D surface_extent);
    std::optional<PresentationTarget> acquire(std::size_t frame_slot);
    void record_copy(VkCommandBuffer commands, const Image& transfer_source, const PresentationTarget& target) const;
    void present(const PresentationTarget& target);

private:
    struct SlotSemaphores {
        DeviceHandle<VkSemaphore> image_acquired;
        DeviceHandle<VkSemaphore> frame_copied;
    };

    void replace_swapchain();

    const Device& device_;
    VkSurfaceKHR surface_;
    VkExtent2D surface_extent_;
    FrameLifecycle lifecycle_;
    std::vector<SlotSemaphores> slots_;
    std::optional<Swapchain> swapchain_;
};

}
