#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

std::uint32_t find_memory_type(VkPhysicalDevice device, std::uint32_t allowed_types, VkMemoryPropertyFlags properties);

}
