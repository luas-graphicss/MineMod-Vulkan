#include "terrain/terrain_renderer.hpp"

#include <stdint.h>

#include <algorithm>
#include <array>
#include <iterator>
#include <span>
#include <stdexcept>
#include <vector>

#include "descriptors/descriptor_layout.hpp"
#include "frame/frame_targets.hpp"
#include "frame/viewport.hpp"
#include "pipelines/shader_module.hpp"
#include "terrain/section_selection.hpp"
#include "terrain/section_space.hpp"
#include "terrain/terrain_vertex.hpp"
#include "terrain_frag.h"
#include "terrain_vert.h"
#include "visibility/frustum.hpp"

namespace minemod::renderer {
namespace {

constexpr VkShaderStageFlags constant_stages = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;

struct TerrainConstants {
    std::array<float, 16> view_projection;
    std::array<float, 4> fog_color;
    std::array<float, 3> section_origin;
    float alpha_cutoff;
    float fog_start;
    float fog_end;
    std::uint32_t samples_base_level_only;
    std::uint32_t fog_shape;
};

static_assert(sizeof(TerrainConstants) == 112, "the push constant block mirrors the std430 layout of the shaders");

constexpr std::array texture_bindings{
    DescriptorBinding{0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT},
    DescriptorBinding{1, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT},
};

DeviceHandle<VkPipelineLayout> create_pipeline_layout(const Device& device, VkDescriptorSetLayout textures) {
    const VkPushConstantRange constants{constant_stages, 0, sizeof(TerrainConstants)};
    const VkPipelineLayoutCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,
        .setLayoutCount = 1,
        .pSetLayouts = &textures,
        .pushConstantRangeCount = 1,
        .pPushConstantRanges = &constants,
    };
    return create_device_handle<VkPipelineLayout>(
        device.get(), create_info, vkCreatePipelineLayout, vkDestroyPipelineLayout, "creating the terrain pipeline layout");
}

std::vector<SectionPosition> drawable_sections(const TerrainFrame& frame, const TerrainSections& sections) {
    if (!frame.reachable_sections) {
        return sections.positions();
    }
    std::vector<SectionPosition> published;
    std::ranges::copy_if(
        *frame.reachable_sections, std::back_inserter(published), [&sections](const SectionPosition& section) {
            return sections.find(section) != nullptr;
        });
    return published;
}

class LayerRecorder {
public:
    LayerRecorder(VkCommandBuffer commands, VkPipelineLayout layout, const TerrainFrame& frame, TerrainLayer layer)
        : commands_(commands), layout_(layout), frame_(frame), layer_(layer), state_(layer_state(layer)) {}

    bool draw(const SectionPosition& section, const SectionBuffers& buffers) const {
        const IndexRange& range = buffers.range(layer_);
        if (range.index_count == 0) {
            return false;
        }

        const Vector3 origin = section_origin(section, frame_.camera);
        const TerrainConstants constants{
            frame_.view_projection.columns,
            frame_.fog.color,
            {origin.x, origin.y, origin.z},
            state_.alpha_cutoff,
            frame_.fog.start,
            frame_.fog.end,
            state_.atlas_levels == AtlasLevels::base_only ? 1u : 0u,
            static_cast<std::uint32_t>(frame_.fog.shape),
        };
        vkCmdPushConstants(commands_, layout_, constant_stages, 0, sizeof(constants), &constants);

        const VkBuffer vertices = buffers.vertices();
        const VkDeviceSize first_byte = 0;
        vkCmdBindVertexBuffers(commands_, 0, 1, &vertices, &first_byte);
        vkCmdBindIndexBuffer(commands_, buffers.indices(), 0, VK_INDEX_TYPE_UINT32);
        vkCmdDrawIndexed(commands_, range.index_count, 1, range.first_index, 0, 0);
        return true;
    }

private:
    VkCommandBuffer commands_;
    VkPipelineLayout layout_;
    const TerrainFrame& frame_;
    TerrainLayer layer_;
    LayerState state_;
};

}

TerrainRenderer::TerrainRenderer(const Device& device, PipelineCache& pipelines, VkFormat depth_format)
    : device_(device.get()),
      pipelines_(pipelines),
      depth_format_(depth_format),
      vertex_shader_(create_shader_module(device, SpirvModule{terrain_vert})),
      fragment_shader_(create_shader_module(device, SpirvModule{terrain_frag})),
      textures_layout_(create_descriptor_layout(device, texture_bindings)),
      pipeline_layout_(create_pipeline_layout(device, textures_layout_.get())) {}

VkDescriptorSet TerrainRenderer::bind_textures(DescriptorPools& pools, const TerrainTextures& textures) const {
    const VkDescriptorSet bound = pools.allocate(textures_layout_.get());
    const std::array images{
        VkDescriptorImageInfo{textures.atlas_sampler, textures.atlas, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL},
        VkDescriptorImageInfo{textures.lightmap_sampler, textures.lightmap, VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL},
    };

    std::array<VkWriteDescriptorSet, 2> writes{};
    for (std::uint32_t binding = 0; binding < writes.size(); ++binding) {
        writes[binding] = VkWriteDescriptorSet{
            .sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET,
            .dstSet = bound,
            .dstBinding = binding,
            .descriptorCount = 1,
            .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
            .pImageInfo = &images[binding],
        };
    }
    vkUpdateDescriptorSets(device_, static_cast<std::uint32_t>(writes.size()), writes.data(), 0, nullptr);
    return bound;
}

std::uint32_t TerrainRenderer::record(
    VkCommandBuffer commands,
    const TerrainFrame& frame,
    const TerrainSections& sections,
    std::span<const TerrainLayer> layers) const {
    if (!has_depth(frame.fog)) {
        throw std::invalid_argument("terrain fog must end farther from the camera than it starts");
    }

    const std::vector<SectionPosition> visible =
        visible_sections(drawable_sections(frame, sections), Frustum(frame.view_projection), frame.camera);

    set_upright_viewport(commands, frame.extent);
    vkCmdBindDescriptorSets(
        commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline_layout_.get(), 0, 1, &frame.textures, 0, nullptr);

    std::uint32_t draw_count = 0;
    for (const TerrainLayer layer : layers) {
        const LayerState state = layer_state(layer);
        const LayerRecorder recorder(commands, pipeline_layout_.get(), frame, layer);
        vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipelines_.pipeline_for(pipeline_state(state)));
        for (const SectionPosition& section : ordered_sections(visible, frame.camera, state.direction)) {
            draw_count += recorder.draw(section, *sections.find(section)) ? 1u : 0u;
        }
    }
    return draw_count;
}

GraphicsPipelineState TerrainRenderer::pipeline_state(const LayerState& state) const {
    return GraphicsPipelineState{
        .vertex_shader = vertex_shader_.get(),
        .fragment_shader = fragment_shader_.get(),
        .layout = pipeline_layout_.get(),
        .vertex_layout = terrain_vertex_layout(),
        .topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
        .cull_mode = VK_CULL_MODE_BACK_BIT,
        .depth = state.depth,
        .blending = state.blending,
        .color_format = frame_color_format,
        .depth_format = depth_format_,
    };
}

}
