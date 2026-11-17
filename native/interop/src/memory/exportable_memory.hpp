#pragma once

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "memory/exported_handle.hpp"

namespace minemod::interop {

class MemoryExport {
public:
    explicit MemoryExport(VkDevice device);

    ExportedHandle export_handle(VkDeviceMemory memory) const;

private:
    VkDevice device_;
    PFN_vkVoidFunction export_memory_;
};

renderer::DeviceHandle<VkDeviceMemory> allocate_exportable_memory(
    const renderer::Device& device,
    const VkMemoryRequirements& requirements,
    const VkMemoryDedicatedAllocateInfo& dedicated_to);

}
