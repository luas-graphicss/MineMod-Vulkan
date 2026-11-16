#pragma once

#include <cstdint>
#include <span>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"

namespace minemod::renderer {

struct DescriptorBinding {
    std::uint32_t binding;
    VkDescriptorType type;
    VkShaderStageFlags stages;
};

DeviceHandle<VkDescriptorSetLayout> create_descriptor_layout(
    const Device& device, std::span<const DescriptorBinding> bindings);

}
