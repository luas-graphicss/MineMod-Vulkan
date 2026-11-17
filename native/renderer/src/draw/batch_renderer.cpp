#include "draw/batch_renderer.hpp"

#include <algorithm>
#include <cstdint>
#include <iterator>

#include "descriptors/descriptor_layout.hpp"
#include "draw/element_defaults.hpp"
#include "frame/frame_rendering.hpp"
#include "frame/frame_targets.hpp"
#include "frame/viewport.hpp"
#include "pipelines/shader_module.hpp"
#include "scene_frag.h"
#include "scene_vert.h"

namespace minemod::renderer {
namespace {

constexpr VkShaderStageFlags constant_stages = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
constexpr std::uint32_t view_set = 0;
constexpr std::uint32_t texture_set = 1;

struct DrawConstants {
    std::array<float, 16> model_view;
    std::array<float, 4> color_modulator;
    float alpha_cutoff;
    std::uint32_t shading;
    std::array<float, 2> frame_size;
    float line_width;
    TextureTransform texture_transform;
};

static_assert(sizeof(DrawConstants) == 124, "the push constant block mirrors the std430 layout of the shaders");

constexpr std::array view_bindings{
    DescriptorBinding{0, VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, constant_stages},
    DescriptorBinding{1, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_VERTEX_BIT},
    DescriptorBinding{2, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_VERTEX_BIT},
};

constexpr std::array texture_bindings{
    DescriptorBinding{0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT},
};

DeviceHandle<VkPipelineLayout> create_pipeline_layout(
    const Device& device, VkDescriptorSetLayout view, VkDescriptorSetLayout texture) {
    const std::array set_layouts{view, texture};
    const VkPushConstantRange constants{constant_stages, 0, sizeof(DrawConstants)};
    const VkPipelineLayoutCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,
        .setLayoutCount = static_cast<std::uint32_t>(set_layouts.size()),
        .pSetLayouts = set_layouts.data(),
        .pushConstantRangeCount = 1,
        .pPushConstantRanges = &constants,
    };
    return create_device_handle<VkPipelineLayout>(
        device.get(), create_info, vkCreatePipelineLayout, vkDestroyPipelineLayout, "creating the batch pipeline layout");
}

Buffer create_element_defaults(const Device& device) {
    const ElementDefaults defaults;
    Buffer buffer(device, sizeof(ElementDefaults), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, host_memory);
    buffer.write(std::as_bytes(std::span(&defaults, 1)));
    return buffer;
}

VkWriteDescriptorSet sampled_image_write(VkDescriptorSet set, std::uint32_t binding, const VkDescriptorImageInfo& image) {
    return VkWriteDescriptorSet{
        .sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET,
        .dstSet = set,
        .dstBinding = binding,
        .descriptorCount = 1,
        .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
        .pImageInfo = &image,
    };
}

VkCullModeFlags cull_mode(FaceCulling culling) noexcept {
    return culling == FaceCulling::back_faces ? VK_CULL_MODE_BACK_BIT : VK_CULL_MODE_NONE;
}

}

BatchRenderer::BatchRenderer(const Device& device, PipelineCache& pipelines, VkFormat depth_format)
    : device_(device.get()),
      pipelines_(pipelines),
      depth_format_(depth_format),
      vertex_shader_(create_shader_module(device, SpirvModule{scene_vert})),
      fragment_shader_(create_shader_module(device, SpirvModule{scene_frag})),
      view_layout_(create_descriptor_layout(device, view_bindings)),
      texture_layout_(create_descriptor_layout(device, texture_bindings)),
      pipeline_layout_(create_pipeline_layout(device, view_layout_.get(), texture_layout_.get())),
      element_defaults_(create_element_defaults(device)) {}

VkDescriptorSet BatchRenderer::bind_view(
    DescriptorPools& pools, const ViewConstants& constants, const ViewTextures& textures) const {
    const VkDescriptorSet bound = pools.allocate(view_layout_.get());
    const VkDescriptorBufferInfo block{constants.buffer(), 0, view_constants_bytes};
    const VkDescriptorImageInfo lightmap{
        textures.lightmap_sampler, textures.lightmap, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL};
    const VkDescriptorImageInfo overlay{
        textures.overlay_sampler, textures.overlay, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL};

    const std::array writes{
        VkWriteDescriptorSet{
            .sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET,
            .dstSet = bound,
            .dstBinding = 0,
            .descriptorCount = 1,
            .descriptorType = VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER,
            .pBufferInfo = &block,
        },
        sampled_image_write(bound, 1, lightmap),
        sampled_image_write(bound, 2, overlay),
    };
    vkUpdateDescriptorSets(device_, static_cast<std::uint32_t>(writes.size()), writes.data(), 0, nullptr);
    return bound;
}

VkDescriptorSet BatchRenderer::bind_texture(DescriptorPools& pools, VkImageView texture, VkSampler sampler) const {
    const VkDescriptorSet bound = pools.allocate(texture_layout_.get());
    const VkDescriptorImageInfo image{sampler, texture, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL};
    const VkWriteDescriptorSet write = sampled_image_write(bound, 0, image);
    vkUpdateDescriptorSets(device_, 1, &write, 0, nullptr);
    return bound;
}

void BatchRenderer::record(VkCommandBuffer commands, const BatchPass& pass, std::span<const DrawBatch> batches) {
    set_upright_viewport(commands, pass.extent);
    VkDescriptorSet bound_view = VK_NULL_HANDLE;
    const VkRect2D whole_frame{{0, 0}, pass.extent};

    const std::array frame_size{static_cast<float>(pass.extent.width), static_cast<float>(pass.extent.height)};
    for (const DrawBatch& batch : batches) {
        if (batch.clears_depth) {
            clear_frame_depth(commands, pass.extent);
        }
        const Material& material = batch.material;
        const VkDescriptorSet view = batch.view == VK_NULL_HANDLE ? pass.view : batch.view;
        if (view != bound_view) {
            vkCmdBindDescriptorSets(
                commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline_layout_.get(), view_set, 1, &view, 0, nullptr);
            bound_view = view;
        }
        const PipelineKey key{
            batch.program.value_or(BatchProgram{vertex_shader_.get(), fragment_shader_.get()}),
            batch.format,
            material.blending,
            material.depth,
            material.culling,
            material.depth_bias,
            material.writes_color,
        };
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline_for(key));
        vkCmdBindDescriptorSets(
            commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline_layout_.get(), texture_set, 1, &batch.texture, 0, nullptr);

        const DrawConstants constants{
            batch.model_view.columns,
            batch.color_modulator,
            material.alpha_cutoff,
            shading_bits(material.shading),
            frame_size,
            batch.line_width,
            batch.texture_transform,
        };
        vkCmdPushConstants(commands, pipeline_layout_.get(), constant_stages, 0, sizeof(constants), &constants);

        const VkRect2D clip = batch.clip.value_or(whole_frame);
        vkCmdSetScissor(commands, 0, 1, &clip);

        const std::array vertex_buffers{batch.geometry.buffer, element_defaults_.get()};
        const std::array<VkDeviceSize, 2> first_bytes{batch.geometry.vertices.offset, 0};
        vkCmdBindVertexBuffers(commands, per_vertex_binding, 2, vertex_buffers.data(), first_bytes.data());
        vkCmdBindIndexBuffer(commands, batch.geometry.buffer, batch.geometry.indices.offset, VK_INDEX_TYPE_UINT32);
        vkCmdDrawIndexed(commands, batch.geometry.index_count, 1, 0, 0, 0);
    }
}

VkPipeline BatchRenderer::pipeline_for(const PipelineKey& key) {
    const auto used = std::ranges::find(used_pipelines_, key, &std::pair<PipelineKey, VkPipeline>::first);
    if (used != used_pipelines_.end()) {
        return used->second;
    }

    const VkPipeline pipeline = pipelines_.pipeline_for(GraphicsPipelineState{
        .vertex_shader = key.program.vertex,
        .fragment_shader = key.program.fragment,
        .layout = pipeline_layout_.get(),
        .vertex_layout = key.format.layout(),
        .topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
        .cull_mode = cull_mode(key.culling),
        .depth = key.depth,
        .blending = key.blending,
        .color_format = frame_color_format,
        .depth_format = depth_format_,
        .depth_bias = key.depth_bias,
        .writes_color = key.writes_color,
    });
    used_pipelines_.emplace_back(key, pipeline);
    return pipeline;
}

std::vector<DeviceHandle<VkPipeline>> BatchRenderer::take_pipelines_of(const BatchProgram& program) {
    std::erase_if(used_pipelines_, [&program](const std::pair<PipelineKey, VkPipeline>& used) {
        return used.first.program == program;
    });
    std::vector<DeviceHandle<VkPipeline>> taken = pipelines_.take_pipelines_using(program.vertex);
    std::vector<DeviceHandle<VkPipeline>> sharing_fragment = pipelines_.take_pipelines_using(program.fragment);
    taken.insert(
        taken.end(),
        std::make_move_iterator(sharing_fragment.begin()),
        std::make_move_iterator(sharing_fragment.end()));
    return taken;
}

}
