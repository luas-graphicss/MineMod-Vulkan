#pragma once

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "memory/owned_descriptor.hpp"

namespace minemod::interop {

class MemoryExport {
public:
    explicit MemoryExport(VkDevice device);

    OwnedDescriptor export_descriptor(VkDeviceMemory memory) const;

private:
    VkDevice device_;
    PFN_vkGetMemoryFdKHR get_memory_descriptor_;
};

renderer::DeviceHandle<VkDeviceMemory> allocate_exportable_memory(
    const renderer::Device& device,
    const VkMemoryRequirements& requirements,
    const VkMemoryDedicatedAllocateInfo& dedicated_to);

}
