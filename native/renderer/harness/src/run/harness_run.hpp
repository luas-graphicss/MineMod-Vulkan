#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/instance.hpp"
#include "device/physical_device.hpp"
#include "frame/frame_readback.hpp"

namespace minemod::harness {

struct HarnessOptions {
    renderer::Validation validation = renderer::Validation::disabled;
    VkExtent2D extent{256, 256};
    std::uint32_t presented_frames = 1;
};

struct HarnessReport {
    renderer::DeviceDescription device;
    renderer::Validation validation;
    VkFormat presentation_format;
    std::uint32_t presented_frames;
    renderer::FramePixels frame;
    std::vector<std::string> validation_messages;
};

HarnessReport run_harness(const HarnessOptions& options);

}
