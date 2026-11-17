#pragma once

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "memory/exported_handle.hpp"

namespace minemod::interop::test_support {

renderer::DeviceHandle<VkDeviceMemory> import_image_memory(
    const renderer::Device& device, VkImage image, VkDeviceSize allocation_bytes, ExportedHandle memory);

}
