#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"

namespace minemod::renderer {

struct ImageDescription {
    VkExtent2D extent;
    VkFormat format;
    VkImageUsageFlags usage;
    VkImageAspectFlags aspect;
    std::uint32_t mip_levels = 1;
};

class Image {
public:
    Image(const Device& device, const ImageDescription& description);

    VkImage get() const noexcept {
        return image_.get();
    }

    VkImageView view() const noexcept {
        return view_.get();
    }

    VkExtent2D extent() const noexcept {
        return description_.extent;
    }

private:
    ImageDescription description_;
    DeviceHandle<VkDeviceMemory> memory_;
    DeviceHandle<VkImage> image_;
    DeviceHandle<VkImageView> view_;
};

}
