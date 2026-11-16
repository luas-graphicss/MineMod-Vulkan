#pragma once

#include <cstddef>
#include <cstdint>
#include <memory>
#include <optional>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "presentation/frame_lifecycle.hpp"
#include "presentation/present_pacing.hpp"
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
    void set_pacing(PresentPacing pacing) noexcept;
    std::optional<PresentationTarget> acquire(std::size_t frame_slot);
    void record_copy(VkCommandBuffer commands, const Image& transfer_source, const PresentationTarget& target) const;
    void present(const PresentationTarget& target);

private:
    void replace_swapchain();

    const Device& device_;
    VkSurfaceKHR surface_;
    VkExtent2D surface_extent_;
    PresentPacing pacing_ = PresentPacing::vsync;
    FrameLifecycle lifecycle_;
    std::vector<DeviceHandle<VkSemaphore>> acquired_;
    std::unique_ptr<Swapchain> swapchain_;
};

}
