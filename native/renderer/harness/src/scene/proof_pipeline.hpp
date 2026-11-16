#pragma once

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"

namespace minemod::harness {

renderer::DeviceHandle<VkPipeline> create_proof_pipeline(const renderer::Device& device, VkPipelineLayout layout);

}
