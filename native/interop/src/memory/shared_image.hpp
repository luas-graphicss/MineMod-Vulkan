#pragma once

#include <array>
#include <cstdint>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "memory/exportable_memory.hpp"
#include "memory/exported_handle.hpp"

namespace minemod::interop {

extern const std::array<const char*, 1> shared_image_extensions;

struct SharedImageDescription {
    VkExtent2D extent;
    VkFormat format;
    VkImageUsageFlags usage;
    std::uint32_t mip_levels = 1;
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

    ExportedHandle export_memory() const;

private:
    MemoryExport memory_export_;
    VkDeviceSize allocation_size_ = 0;
    renderer::DeviceHandle<VkDeviceMemory> memory_;
    renderer::DeviceHandle<VkImage> image_;
};

}
