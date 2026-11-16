#include "scene/proof_pipeline.hpp"

#include <stdint.h>

#include <array>
#include <cstddef>

#include "device/vulkan_error.hpp"
#include "frame/frame_targets.hpp"
#include "pipelines/shader_module.hpp"
#include "proof_scene_frag.h"
#include "proof_scene_vert.h"
#include "scene/proof_geometry.hpp"

namespace minemod::harness {

namespace {

VkPipelineShaderStageCreateInfo shader_stage(VkShaderStageFlagBits stage, VkShaderModule shader) {
    return VkPipelineShaderStageCreateInfo{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO,
        .stage = stage,
        .module = shader,
        .pName = "main",
    };
}

}

renderer::DeviceHandle<VkPipeline> create_proof_pipeline(const renderer::Device& device, VkPipelineLayout layout) {
    const auto vertex_shader = renderer::create_shader_module(device, proof_scene_vert);
    const auto fragment_shader = renderer::create_shader_module(device, proof_scene_frag);
    const std::array stages{
        shader_stage(VK_SHADER_STAGE_VERTEX_BIT, vertex_shader.get()),
        shader_stage(VK_SHADER_STAGE_FRAGMENT_BIT, fragment_shader.get()),
    };

    const VkVertexInputBindingDescription vertex_binding{
        .binding = 0,
        .stride = sizeof(ProofVertex),
        .inputRate = VK_VERTEX_INPUT_RATE_VERTEX,
    };
    const std::array vertex_attributes{
        VkVertexInputAttributeDescription{0, 0, VK_FORMAT_R32G32B32_SFLOAT, offsetof(ProofVertex, position)},
        VkVertexInputAttributeDescription{1, 0, VK_FORMAT_R32G32_SFLOAT, offsetof(ProofVertex, texture_coordinate)},
        VkVertexInputAttributeDescription{2, 0, VK_FORMAT_R32G32B32A32_SFLOAT, offsetof(ProofVertex, tint)},
    };
    const VkPipelineVertexInputStateCreateInfo vertex_input{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO,
        .vertexBindingDescriptionCount = 1,
        .pVertexBindingDescriptions = &vertex_binding,
        .vertexAttributeDescriptionCount = static_cast<std::uint32_t>(vertex_attributes.size()),
        .pVertexAttributeDescriptions = vertex_attributes.data(),
    };
    const VkPipelineInputAssemblyStateCreateInfo input_assembly{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO,
        .topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
    };
    const VkPipelineViewportStateCreateInfo viewport{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO,
        .viewportCount = 1,
        .scissorCount = 1,
    };
    const VkPipelineRasterizationStateCreateInfo rasterization{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO,
        .polygonMode = VK_POLYGON_MODE_FILL,
        .cullMode = VK_CULL_MODE_NONE,
        .frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE,
        .lineWidth = 1.0f,
    };
    const VkPipelineMultisampleStateCreateInfo multisample{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO,
        .rasterizationSamples = VK_SAMPLE_COUNT_1_BIT,
    };
    const VkPipelineDepthStencilStateCreateInfo depth{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO,
        .depthTestEnable = VK_TRUE,
        .depthWriteEnable = VK_TRUE,
        .depthCompareOp = VK_COMPARE_OP_LESS,
    };
    const VkPipelineColorBlendAttachmentState color_attachment{
        .colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT
            | VK_COLOR_COMPONENT_A_BIT,
    };
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
    const VkFormat color_format = renderer::frame_color_format;
    const VkPipelineRenderingCreateInfo rendering{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_RENDERING_CREATE_INFO,
        .colorAttachmentCount = 1,
        .pColorAttachmentFormats = &color_format,
        .depthAttachmentFormat = renderer::frame_depth_format,
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
        .layout = layout,
    };

    VkPipeline pipeline = VK_NULL_HANDLE;
    renderer::require_success(
        vkCreateGraphicsPipelines(device.get(), VK_NULL_HANDLE, 1, &create_info, nullptr, &pipeline),
        "creating the proof scene pipeline");
    return renderer::DeviceHandle<VkPipeline>(device.get(), pipeline, vkDestroyPipeline);
}

}
