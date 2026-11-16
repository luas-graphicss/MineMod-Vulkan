#pragma once

#include <array>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "memory/owned_descriptor.hpp"

namespace minemod::interop {

inline constexpr std::array<const char*, 1> shared_image_extensions{VK_KHR_EXTERNAL_MEMORY_FD_EXTENSION_NAME};

struct SharedImageDescription {
    VkExtent2D extent;
    VkFormat format;
    VkImageUsageFlags usage;
};

class SharedImage {
public:
    SharedImage(const renderer::Device& device, const SharedImageDescription& description);

    VkImage get() const noexcept {
        return image_.get();
    }

    VkDeviceSize allocation_size() const noexcept {
        return allocation_size_;
    }

    OwnedDescriptor export_memory() const;

private:
    VkDevice device_;
    PFN_vkGetMemoryFdKHR get_memory_descriptor_;
    VkDeviceSize allocation_size_ = 0;
    renderer::DeviceHandle<VkDeviceMemory> memory_;
    renderer::DeviceHandle<VkImage> image_;
};

}
