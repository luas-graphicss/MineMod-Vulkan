#include "resources/memory_type.hpp"

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

std::uint32_t find_memory_type(VkPhysicalDevice device, std::uint32_t allowed_types, VkMemoryPropertyFlags properties) {
    VkPhysicalDeviceMemoryProperties memory{};
    vkGetPhysicalDeviceMemoryProperties(device, &memory);
    for (std::uint32_t type = 0; type < memory.memoryTypeCount; ++type) {
        const bool is_allowed = (allowed_types & (std::uint32_t{1} << type)) != 0;
        if (is_allowed && (memory.memoryTypes[type].propertyFlags & properties) == properties) {
            return type;
        }
    }
    throw UnsupportedSystem("the device has no memory type with the requested properties");
}

}
