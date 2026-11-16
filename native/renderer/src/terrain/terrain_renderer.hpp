#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

#include "descriptors/descriptor_pools.hpp"
#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "pipelines/graphics_pipeline_state.hpp"
#include "pipelines/pipeline_cache.hpp"
#include "terrain/terrain_frame.hpp"
#include "terrain/terrain_layer.hpp"
#include "terrain/terrain_sections.hpp"

namespace minemod::renderer {

struct TerrainTextures {
    VkImageView atlas;
    VkSampler atlas_sampler;
    VkImageView lightmap;
    VkSampler lightmap_sampler;
};

class TerrainRenderer {
public:
    TerrainRenderer(const Device& device, PipelineCache& pipelines);

    VkDescriptorSet bind_textures(DescriptorPools& pools, const TerrainTextures& textures) const;
    std::uint32_t record(VkCommandBuffer commands, const TerrainFrame& frame, const TerrainSections& sections) const;

private:
    GraphicsPipelineState pipeline_state(const LayerState& state) const;

    VkDevice device_;
    PipelineCache& pipelines_;
    DeviceHandle<VkShaderModule> vertex_shader_;
    DeviceHandle<VkShaderModule> fragment_shader_;
    DeviceHandle<VkDescriptorSetLayout> textures_layout_;
    DeviceHandle<VkPipelineLayout> pipeline_layout_;
};

}
