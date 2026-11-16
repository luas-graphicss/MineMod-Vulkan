#pragma once

#include <vulkan/vulkan.h>

namespace minemod::interop {

inline constexpr VkExternalMemoryHandleTypeFlagBits shared_handle_type = VK_EXTERNAL_MEMORY_HANDLE_TYPE_OPAQUE_FD_BIT;

struct ExternalSupport {
    bool exportable;
    VkExtent2D max_extent;
};

ExternalSupport query_external_support(VkPhysicalDevice device, VkFormat format, VkImageUsageFlags usage);

}
