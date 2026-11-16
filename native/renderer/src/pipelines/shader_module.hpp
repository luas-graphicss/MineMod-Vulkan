#pragma once

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "shaders/spirv_module.hpp"

namespace minemod::renderer {

DeviceHandle<VkShaderModule> create_shader_module(const Device& device, const SpirvModule& module);

}
