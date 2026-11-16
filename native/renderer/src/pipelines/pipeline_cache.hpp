#pragma once

#include <cstddef>
#include <unordered_map>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "pipelines/graphics_pipeline_state.hpp"

namespace minemod::renderer {

class PipelineCache {
public:
    explicit PipelineCache(const Device& device) noexcept : device_(device.get()) {}

    VkPipeline pipeline_for(const GraphicsPipelineState& state);

    std::size_t pipeline_count() const noexcept {
        return pipelines_.size();
    }

private:
    VkDevice device_;
    std::unordered_map<GraphicsPipelineState, DeviceHandle<VkPipeline>, GraphicsPipelineStateHash> pipelines_;
};

}
