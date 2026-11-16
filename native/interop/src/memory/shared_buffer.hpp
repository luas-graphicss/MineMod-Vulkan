#pragma once

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "memory/exportable_memory.hpp"
#include "memory/owned_descriptor.hpp"

namespace minemod::interop {

struct SharedBufferDescription {
    VkDeviceSize size_bytes;
    VkBufferUsageFlags usage;
};

bool can_export_buffers(VkPhysicalDevice device, VkBufferUsageFlags usage);

class SharedBuffer {
public:
    SharedBuffer(const renderer::Device& device, const SharedBufferDescription& description);

    VkBuffer get() const noexcept {
        return buffer_.get();
    }

    VkDeviceSize allocation_size() const noexcept {
        return allocation_size_;
    }

    OwnedDescriptor export_memory() const;

private:
    MemoryExport memory_export_;
    VkDeviceSize allocation_size_ = 0;
    renderer::DeviceHandle<VkDeviceMemory> memory_;
    renderer::DeviceHandle<VkBuffer> buffer_;
};

}
