#pragma once

#include <cstddef>
#include <span>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"

namespace minemod::renderer {

inline constexpr VkMemoryPropertyFlags host_memory =
    VK_MEMORY_PROPERTY_HOST_VISIBLE_BIT | VK_MEMORY_PROPERTY_HOST_COHERENT_BIT;

class Buffer {
public:
    Buffer(const Device& device, VkDeviceSize size_bytes, VkBufferUsageFlags usage, VkMemoryPropertyFlags memory);

    VkBuffer get() const noexcept {
        return buffer_.get();
    }

    void write(std::span<const std::byte> bytes, VkDeviceSize offset = 0) const;
    std::vector<std::byte> read() const;

private:
    VkDevice device_;
    VkDeviceSize size_bytes_;
    DeviceHandle<VkDeviceMemory> memory_;
    DeviceHandle<VkBuffer> buffer_;
};

}
