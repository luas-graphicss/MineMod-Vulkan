#pragma once

#include <cstdint>
#include <span>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"

namespace minemod::renderer {

DeviceHandle<VkShaderModule> create_shader_module(const Device& device, std::span<const std::uint32_t> code);

}
