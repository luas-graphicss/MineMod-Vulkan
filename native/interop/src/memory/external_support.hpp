#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

namespace minemod::interop {

inline constexpr VkExternalMemoryHandleTypeFlagBits shared_handle_type = VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT;

struct ExternalSupport {
    bool exportable;
    VkExtent2D max_extent;
    std::uint32_t max_mip_levels;
};

ExternalSupport query_external_support(VkPhysicalDevice device, VkFormat format, VkImageUsageFlags usage);

}
