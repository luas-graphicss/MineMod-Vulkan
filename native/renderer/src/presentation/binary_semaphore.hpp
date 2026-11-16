#pragma once

#include <vulkan/vulkan.h>

#include "device/device_handle.hpp"

namespace minemod::renderer {

DeviceHandle<VkSemaphore> create_binary_semaphore(VkDevice device);

}
