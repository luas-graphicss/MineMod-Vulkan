#include "effects/frame_blur.hpp"

#include <array>
#include <cmath>

#include "blur_frag.h"
#include "blur_vert.h"
#include "descriptors/descriptor_layout.hpp"
#include "frame/frame_targets.hpp"
#include "pipelines/shader_module.hpp"

namespace minemod::renderer {

namespace {

constexpr std::uint32_t corners_covering_the_target = 3;

struct BlurConstants {
    std::array<float, 2> sample_step;
    float radius;
};

constexpr std::array source_bindings{
    DescriptorBinding{0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT},
};

DeviceHandle<VkPipelineLayout> create_pipeline_layout(const Device& device, VkDescriptorSetLayout source) {
    const VkPushConstantRange constants{VK_SHADER_STAGE_FRAGMENT_BIT, 0, sizeof(BlurConstants)};
    const VkPipelineLayoutCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,
        .setLayoutCount = 1,
        .pSetLayouts = &source,
        .pushConstantRangeCount = 1,
        .pPushConstantRanges = &constants,
    };
    return create_device_handle<VkPipelineLayout>(
        device.get(), create_info, vkCreatePipelineLayout, vkDestroyPipelineLayout, "creating the blur pipeline layout");
}

std::array<float, 2> step_along(BlurAxis axis, VkExtent2D extent) noexcept {
    if (axis == BlurAxis::across) {
        return {1.0f / static_cast<float>(extent.width), 0.0f};
    }
    return {0.0f, 1.0f / static_cast<float>(extent.height)};
}

void cover_the_target(VkCommandBuffer commands, VkExtent2D extent) {
    const VkViewport viewport{
        0.0f, 0.0f, static_cast<float>(extent.width), static_cast<float>(extent.height), 0.0f, 1.0f};
    const VkRect2D scissor{{0, 0}, extent};
    vkCmdSetViewport(commands, 0, 1, &viewport);
    vkCmdSetScissor(commands, 0, 1, &scissor);
}

}

FrameBlurRenderer::FrameBlurRenderer(const Device& device, PipelineCache& pipelines, VkSampler smooth_sampler)
    : device_(device.get()),
      pipelines_(pipelines),
      smooth_sampler_(smooth_sampler),
      vertex_shader_(create_shader_module(device, SpirvModule{blur_vert})),
      fragment_shader_(create_shader_module(device, SpirvModule{blur_frag})),
      source_layout_(create_descriptor_layout(device, source_bindings)),
      pipeline_layout_(create_pipeline_layout(device, source_layout_.get())) {}

VkDescriptorSet FrameBlurRenderer::bind_source(DescriptorPools& pools, VkImageView source) const {
    const VkDescriptorSet bound = pools.allocate(source_layout_.get());
    const VkDescriptorImageInfo image{smooth_sampler_, source, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL};
    const VkWriteDescriptorSet write{
        .sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET,
        .dstSet = bound,
        .dstBinding = 0,
        .descriptorCount = 1,
        .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
        .pImageInfo = &image,
    };
    vkUpdateDescriptorSets(device_, 1, &write, 0, nullptr);
    return bound;
}

void FrameBlurRenderer::record(
    VkCommandBuffer commands, VkDescriptorSet source, const BoundImage& target, BlurAxis axis, float radius) const {
    const VkRenderingAttachmentInfo color{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = target.view,
        .imageLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
        .loadOp = VK_ATTACHMENT_LOAD_OP_DONT_CARE,
        .storeOp = VK_ATTACHMENT_STORE_OP_STORE,
    };
    const VkRenderingInfo rendering{
        .sType = VK_STRUCTURE_TYPE_RENDERING_INFO,
        .renderArea = {{0, 0}, target.extent},
        .layerCount = 1,
        .colorAttachmentCount = 1,
        .pColorAttachments = &color,
    };
    const BlurConstants constants{step_along(axis, target.extent), std::round(radius)};
    vkCmdBeginRendering(commands, &rendering);
    cover_the_target(commands, target.extent);
    vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline());
    vkCmdBindDescriptorSets(
        commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline_layout_.get(), 0, 1, &source, 0, nullptr);
    vkCmdPushConstants(
        commands, pipeline_layout_.get(), VK_SHADER_STAGE_FRAGMENT_BIT, 0, sizeof(constants), &constants);
    vkCmdDraw(commands, corners_covering_the_target, 1, 0, 0);
    vkCmdEndRendering(commands);
}

VkPipeline FrameBlurRenderer::pipeline() const {
    return pipelines_.pipeline_for(GraphicsPipelineState{
        .vertex_shader = vertex_shader_.get(),
        .fragment_shader = fragment_shader_.get(),
        .layout = pipeline_layout_.get(),
        .vertex_layout = VertexLayout{0, {}},
        .topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
        .cull_mode = VK_CULL_MODE_NONE,
        .depth = DepthState{false, false, VK_COMPARE_OP_ALWAYS},
        .blending = Blending::disabled,
        .color_format = frame_color_format,
        .depth_format = VK_FORMAT_UNDEFINED,
    });
}

}
