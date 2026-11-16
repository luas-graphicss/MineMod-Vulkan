#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

#include "descriptors/descriptor_pools.hpp"
#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "graph/graph_images.hpp"
#include "pipelines/pipeline_cache.hpp"

namespace minemod::renderer {

struct FrameBlur {
    float radius;
    std::uint32_t repeats;
};

enum class BlurAxis : std::uint8_t { across, down };

class FrameBlurRenderer {
public:
    FrameBlurRenderer(const Device& device, PipelineCache& pipelines, VkSampler smooth_sampler);

    VkDescriptorSet bind_source(DescriptorPools& pools, VkImageView source) const;
    void record(
        VkCommandBuffer commands, VkDescriptorSet source, const BoundImage& target, BlurAxis axis, float radius) const;

private:
    VkPipeline pipeline() const;

    VkDevice device_;
    PipelineCache& pipelines_;
    VkSampler smooth_sampler_;
    DeviceHandle<VkShaderModule> vertex_shader_;
    DeviceHandle<VkShaderModule> fragment_shader_;
    DeviceHandle<VkDescriptorSetLayout> source_layout_;
    DeviceHandle<VkPipelineLayout> pipeline_layout_;
};

}
