#include "pipelines/pipeline_cache.hpp"

#include <array>
#include <cstdint>
#include <optional>
#include <utility>
#include <vector>

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

namespace {

VkPipelineShaderStageCreateInfo shader_stage(VkShaderStageFlagBits stage, VkShaderModule shader) {
    return VkPipelineShaderStageCreateInfo{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO,
        .stage = stage,
        .module = shader,
        .pName = "main",
    };
}

std::vector<VkVertexInputAttributeDescription> attribute_descriptions(const VertexLayout& layout) {
    std::vector<VkVertexInputAttributeDescription> descriptions;
    descriptions.reserve(layout.attributes.size());
    for (const VertexAttribute& attribute : layout.attributes) {
        const std::uint32_t binding =
            attribute.source == VertexSource::shared_constant ? shared_constant_binding : per_vertex_binding;
        descriptions.push_back({attribute.location, binding, attribute.format, attribute.offset});
    }
    return descriptions;
}

std::vector<VkVertexInputBindingDescription> binding_descriptions(const VertexLayout& layout) {
    std::vector<VkVertexInputBindingDescription> descriptions;
    if (layout.stride != 0) {
        descriptions.push_back({per_vertex_binding, layout.stride, VK_VERTEX_INPUT_RATE_VERTEX});
    }
    if (layout.constant_stride != 0) {
        descriptions.push_back({shared_constant_binding, layout.constant_stride, VK_VERTEX_INPUT_RATE_INSTANCE});
    }
    return descriptions;
}

constexpr VkColorComponentFlags every_color_component =
    VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;

bool is_biased(const DepthBias& bias) noexcept {
    return bias.constant != 0.0f || bias.slope != 0.0f;
}

VkPipelineColorBlendAttachmentState blend_attachment(const GraphicsPipelineState& state) {
    VkPipelineColorBlendAttachmentState attachment{
        .colorWriteMask = state.writes_color ? every_color_component : VkColorComponentFlags{0},
    };
    const std::optional<BlendFactors> factors = blend_factors(state.blending);
    if (factors) {
        attachment.blendEnable = VK_TRUE;
        attachment.srcColorBlendFactor = factors->source_color;
        attachment.dstColorBlendFactor = factors->destination_color;
        attachment.colorBlendOp = VK_BLEND_OP_ADD;
        attachment.srcAlphaBlendFactor = factors->source_alpha;
        attachment.dstAlphaBlendFactor = factors->destination_alpha;
        attachment.alphaBlendOp = VK_BLEND_OP_ADD;
    }
    return attachment;
}

DeviceHandle<VkPipeline> create_pipeline(VkDevice device, const GraphicsPipelineState& state) {
    const std::array stages{
        shader_stage(VK_SHADER_STAGE_VERTEX_BIT, state.vertex_shader),
        shader_stage(VK_SHADER_STAGE_FRAGMENT_BIT, state.fragment_shader),
    };
    const std::vector<VkVertexInputBindingDescription> vertex_bindings = binding_descriptions(state.vertex_layout);
    const std::vector<VkVertexInputAttributeDescription> vertex_attributes =
        attribute_descriptions(state.vertex_layout);
    const VkPipelineVertexInputStateCreateInfo vertex_input{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO,
        .vertexBindingDescriptionCount = static_cast<std::uint32_t>(vertex_bindings.size()),
        .pVertexBindingDescriptions = vertex_bindings.data(),
        .vertexAttributeDescriptionCount = static_cast<std::uint32_t>(vertex_attributes.size()),
        .pVertexAttributeDescriptions = vertex_attributes.data(),
    };
    const VkPipelineInputAssemblyStateCreateInfo input_assembly{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO,
        .topology = state.topology,
    };
    const VkPipelineViewportStateCreateInfo viewport{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO,
        .viewportCount = 1,
        .scissorCount = 1,
    };
    const VkPipelineRasterizationStateCreateInfo rasterization{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO,
        .polygonMode = VK_POLYGON_MODE_FILL,
        .cullMode = state.cull_mode,
        .frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE,
        .depthBiasEnable = is_biased(state.depth_bias) ? VK_TRUE : VK_FALSE,
        .depthBiasConstantFactor = state.depth_bias.constant,
        .depthBiasSlopeFactor = state.depth_bias.slope,
        .lineWidth = 1.0f,
    };
    const VkPipelineMultisampleStateCreateInfo multisample{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO,
        .rasterizationSamples = VK_SAMPLE_COUNT_1_BIT,
    };
    const VkPipelineDepthStencilStateCreateInfo depth{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO,
        .depthTestEnable = state.depth.test ? VK_TRUE : VK_FALSE,
        .depthWriteEnable = state.depth.write ? VK_TRUE : VK_FALSE,
        .depthCompareOp = state.depth.compare,
    };
    const VkPipelineColorBlendAttachmentState color_attachment = blend_attachment(state);
    const VkPipelineColorBlendStateCreateInfo color_blend{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO,
        .attachmentCount = 1,
        .pAttachments = &color_attachment,
    };
    const std::array dynamic_states{VK_DYNAMIC_STATE_VIEWPORT, VK_DYNAMIC_STATE_SCISSOR};
    const VkPipelineDynamicStateCreateInfo dynamic_state{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_DYNAMIC_STATE_CREATE_INFO,
        .dynamicStateCount = static_cast<std::uint32_t>(dynamic_states.size()),
        .pDynamicStates = dynamic_states.data(),
    };
    const VkPipelineRenderingCreateInfo rendering{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO,
        .colorAttachmentCount = 1,
        .pColorAttachmentFormats = &state.color_format,
        .depthAttachmentFormat = state.depth_format,
    };
    const VkGraphicsPipelineCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO,
        .pNext = &rendering,
        .stageCount = static_cast<std::uint32_t>(stages.size()),
        .pStages = stages.data(),
        .pVertexInputState = &vertex_input,
        .pInputAssemblyState = &input_assembly,
        .pViewportState = &viewport,
        .pRasterizationState = &rasterization,
        .pMultisampleState = &multisample,
        .pDepthStencilState = &depth,
        .pColorBlendState = &color_blend,
        .pDynamicState = &dynamic_state,
        .layout = state.layout,
    };

    VkPipeline pipeline = VK_NULL_HANDLE;
    require_success(
        vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, 1, &create_info, nullptr, &pipeline),
        "creating a graphics pipeline");
    return DeviceHandle<VkPipeline>(device, pipeline, vkDestroyPipeline);
}

}

VkPipeline PipelineCache::pipeline_for(const GraphicsPipelineState& state) {
    const auto cached = pipelines_.find(state);
    if (cached != pipelines_.end()) {
        return cached->second.get();
    }
    return pipelines_.emplace(state, create_pipeline(device_, state)).first->second.get();
}

std::vector<DeviceHandle<VkPipeline>> PipelineCache::take_pipelines_using(VkShaderModule shader) {
    std::vector<DeviceHandle<VkPipeline>> taken;
    for (auto cached = pipelines_.begin(); cached != pipelines_.end();) {
        if (cached->first.vertex_shader == shader || cached->first.fragment_shader == shader) {
            taken.push_back(std::move(cached->second));
            cached = pipelines_.erase(cached);
        } else {
            ++cached;
        }
    }
    return taken;
}

}
