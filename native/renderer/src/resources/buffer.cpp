#include "resources/buffer.hpp"

#include <cstring>
#include <stdexcept>

#include "device/vulkan_error.hpp"
#include "resources/memory_type.hpp"

namespace minemod::renderer {

Buffer::Buffer(const Device& device, VkDeviceSize size_bytes, VkBufferUsageFlags usage, VkMemoryPropertyFlags memory)
    : device_(device.get()), size_bytes_(size_bytes) {
    const VkBufferCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO,
        .size = size_bytes,
        .usage = usage,
        .sharingMode = VK_SHARING_MODE_EXCLUSIVE,
    };
    buffer_ = create_device_handle<VkBuffer>(device_, create_info, vkCreateBuffer, vkDestroyBuffer, "creating a buffer");

    VkMemoryRequirements requirements{};
    vkGetBufferMemoryRequirements(device_, buffer_.get(), &requirements);
    const VkMemoryAllocateInfo allocation{
        .sType = VK_STRUCTURE_TYPE_MEMORY_ALLOCATE_INFO,
        .allocationSize = requirements.size,
        .memoryTypeIndex = find_memory_type(device.physical(), requirements.memoryTypeBits, memory),
    };
    memory_ = create_device_handle<VkDeviceMemory>(
        device_, allocation, vkAllocateMemory, vkFreeMemory, "allocating buffer memory");
    require_success(vkBindBufferMemory(device_, buffer_.get(), memory_.get(), 0), "binding buffer memory");
}

void Buffer::write(std::span<const std::byte> bytes, VkDeviceSize offset) const {
    if (offset > size_bytes_ || bytes.size() > size_bytes_ - offset) {
        throw std::length_error("the bytes do not fit the buffer");
    }
    void* mapped = nullptr;
    require_success(vkMapMemory(device_, memory_.get(), 0, size_bytes_, 0, &mapped), "mapping buffer memory");
    std::memcpy(static_cast<std::byte*>(mapped) + offset, bytes.data(), bytes.size());
    vkUnmapMemory(device_, memory_.get());
}

std::vector<std::byte> Buffer::read() const {
    void* mapped = nullptr;
    require_success(vkMapMemory(device_, memory_.get(), 0, size_bytes_, 0, &mapped), "mapping buffer memory");
    const auto* const first = static_cast<const std::byte*>(mapped);
    std::vector<std::byte> bytes(first, first + size_bytes_);
    vkUnmapMemory(device_, memory_.get());
    return bytes;
}

}
