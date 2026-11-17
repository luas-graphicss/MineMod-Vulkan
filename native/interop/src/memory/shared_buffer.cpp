#include "memory/shared_buffer.hpp"

#include <stdexcept>
#include <string>

#include "device/vulkan_error.hpp"
#include "memory/external_support.hpp"

namespace minemod::interop {

bool can_export_buffers(VkPhysicalDevice device, VkBufferUsageFlags usage) {
    const VkPhysicalDeviceExternalBufferInfo request{
        .sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_EXTERNAL_BUFFER_INFO,
        .usage = usage,
        .handleType = shared_handle_type,
    };
    VkExternalBufferProperties answer{.sType = VK_STRUCTURE_TYPE_EXTERNAL_BUFFER_PROPERTIES};
    vkGetPhysicalDeviceExternalBufferProperties(device, &request, &answer);
    return (answer.externalMemoryProperties.externalMemoryFeatures & VK_EXTERNAL_MEMORY_FEATURE_EXPORTABLE_BIT) != 0;
}

SharedBuffer::SharedBuffer(const renderer::Device& device, const SharedBufferDescription& description)
    : memory_export_(device.get()) {
    if (description.size_bytes == 0) {
        throw std::invalid_argument("a shared buffer needs at least one byte");
    }
    if (!can_export_buffers(device.physical(), description.usage)) {
        throw renderer::UnsupportedSystem(
            "the device cannot export buffers with usage " + std::to_string(description.usage)
            + " to the OpenGL provider");
    }

    const VkExternalMemoryBufferCreateInfo external_buffer{
        .sType = VK_STRUCTURE_TYPE_EXTERNAL_MEMORY_BUFFER_CREATE_INFO,
        .handleTypes = shared_handle_type,
    };
    const VkBufferCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_BUFFER_CREATE_INFO,
        .pNext = &external_buffer,
        .size = description.size_bytes,
        .usage = description.usage,
        .sharingMode = VK_SHARING_MODE_EXCLUSIVE,
    };
    buffer_ = renderer::create_device_handle<VkBuffer>(
        device.get(), create_info, vkCreateBuffer, vkDestroyBuffer, "creating a shared buffer");

    VkMemoryRequirements requirements{};
    vkGetBufferMemoryRequirements(device.get(), buffer_.get(), &requirements);
    allocation_size_ = requirements.size;
    memory_ = allocate_exportable_memory(
        device, requirements, {.sType = VK_STRUCTURE_TYPE_MEMORY_DEDICATED_ALLOCATE_INFO, .buffer = buffer_.get()});
    renderer::require_success(
        vkBindBufferMemory(device.get(), buffer_.get(), memory_.get(), 0), "binding shared buffer memory");
}

ExportedHandle SharedBuffer::export_memory() const {
    return memory_export_.export_handle(memory_.get());
}

}
