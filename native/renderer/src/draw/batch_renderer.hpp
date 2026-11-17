#pragma once

#include <array>
#include <optional>
#include <span>
#include <utility>
#include <vector>

#include <vulkan/vulkan.h>

#include "descriptors/descriptor_pools.hpp"
#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "draw/batch_geometry.hpp"
#include "draw/material.hpp"
#include "draw/texture_transform.hpp"
#include "draw/vertex_format.hpp"
#include "draw/view_constants.hpp"
#include "frame/frame_targets.hpp"
#include "math/matrix.hpp"
#include "pipelines/graphics_pipeline_state.hpp"
#include "pipelines/pipeline_cache.hpp"
#include "resources/buffer.hpp"

namespace minemod::renderer {

struct ViewTextures {
    VkImageView lightmap;
    VkSampler lightmap_sampler;
    VkImageView overlay;
    VkSampler overlay_sampler;
};

struct BatchProgram {
    VkShaderModule vertex;
    VkShaderModule fragment;

    friend bool operator==(const BatchProgram&, const BatchProgram&) = default;
};

struct DrawBatch {
    VertexFormat format;
    BatchGeometry geometry;
    Material material;
    VkDescriptorSet texture;
    Matrix4 model_view;
    std::array<float, 4> color_modulator;
    float line_width = 1.0f;
    VkDescriptorSet view = VK_NULL_HANDLE;
    TextureTransform texture_transform{};
    std::optional<VkRect2D> clip{};
    std::optional<BatchProgram> program{};
    bool clears_depth = false;
};

struct BatchPass {
    VkExtent2D extent;
    VkDescriptorSet view;
};

class BatchRenderer {
public:
    BatchRenderer(const Device& device, PipelineCache& pipelines, VkFormat depth_format = frame_depth_format);

    VkDescriptorSet bind_view(
        DescriptorPools& pools, const ViewConstants& constants, const ViewTextures& textures) const;
    VkDescriptorSet bind_texture(DescriptorPools& pools, VkImageView texture, VkSampler sampler) const;

    void record(VkCommandBuffer commands, const BatchPass& pass, std::span<const DrawBatch> batches);
    std::vector<DeviceHandle<VkPipeline>> take_pipelines_of(const BatchProgram& program);

private:
    struct PipelineKey {
        BatchProgram program;
        VertexFormat format;
        Blending blending;
        DepthState depth;
        FaceCulling culling;
        DepthBias depth_bias;
        bool writes_color;

        friend bool operator==(const PipelineKey&, const PipelineKey&) = default;
    };

    VkPipeline pipeline_for(const PipelineKey& key);

    VkDevice device_;
    PipelineCache& pipelines_;
    VkFormat depth_format_;
    DeviceHandle<VkShaderModule> vertex_shader_;
    DeviceHandle<VkShaderModule> fragment_shader_;
    DeviceHandle<VkDescriptorSetLayout> view_layout_;
    DeviceHandle<VkDescriptorSetLayout> texture_layout_;
    DeviceHandle<VkPipelineLayout> pipeline_layout_;
    Buffer element_defaults_;
    std::vector<std::pair<PipelineKey, VkPipeline>> used_pipelines_;
};

}
