#include <stdint.h>

#include <array>
#include <cstddef>
#include <vector>

#include <doctest.h>

#include "descriptors/descriptor_layout.hpp"
#include "device/device_handle.hpp"
#include "pipelines/pipeline_cache.hpp"
#include "pipelines/shader_module.hpp"
#include "sprite_frag.h"
#include "sprite_vert.h"
#include "support/validated_device.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

struct SpriteVertex {
    std::array<float, 2> position;
    std::array<float, 2> texture_coordinate;
    std::array<float, 4> tint;
};

constexpr std::array sprite_bindings{
    DescriptorBinding{0, VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, VK_SHADER_STAGE_FRAGMENT_BIT},
};

class SpriteProgram {
public:
    explicit SpriteProgram(const Device& device)
        : vertex_shader_(create_shader_module(device, SpirvModule{sprite_vert})),
          fragment_shader_(create_shader_module(device, SpirvModule{sprite_frag})),
          set_layout_(create_descriptor_layout(device, sprite_bindings)) {
        const VkDescriptorSetLayout set_layout = set_layout_.get();
        const VkPipelineLayoutCreateInfo layout_info{
            .sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,
            .setLayoutCount = 1,
            .pSetLayouts = &set_layout,
        };
        layout_ = create_device_handle<VkPipelineLayout>(
            device.get(), layout_info, vkCreatePipelineLayout, vkDestroyPipelineLayout, "creating a pipeline layout");
    }

    GraphicsPipelineState state(Blending blending) const {
        return GraphicsPipelineState{
            .vertex_shader = vertex_shader_.get(),
            .fragment_shader = fragment_shader_.get(),
            .layout = layout_.get(),
            .vertex_layout =
                VertexLayout{
                    sizeof(SpriteVertex),
                    {
                        {0, VK_FORMAT_R32G32_SFLOAT, offsetof(SpriteVertex, position)},
                        {1, VK_FORMAT_R32G32_SFLOAT, offsetof(SpriteVertex, texture_coordinate)},
                        {2, VK_FORMAT_R32G32B32A32_SFLOAT, offsetof(SpriteVertex, tint)},
                    },
                },
            .topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST,
            .cull_mode = VK_CULL_MODE_NONE,
            .depth = DepthState{false, false, VK_COMPARE_OP_ALWAYS},
            .blending = blending,
            .color_format = VK_FORMAT_R8G8B8A8_UNORM,
            .depth_format = VK_FORMAT_UNDEFINED,
        };
    }

private:
    DeviceHandle<VkShaderModule> vertex_shader_;
    DeviceHandle<VkShaderModule> fragment_shader_;
    DeviceHandle<VkDescriptorSetLayout> set_layout_;
    DeviceHandle<VkPipelineLayout> layout_;
};

}

TEST_CASE("an identical state reuses the pipeline that was already created") {
    const ValidatedDevice gpu;
    const SpriteProgram program(gpu.device());
    PipelineCache cache(gpu.device());

    const VkPipeline created = cache.pipeline_for(program.state(Blending::disabled));
    const VkPipeline reused = cache.pipeline_for(program.state(Blending::disabled));

    CHECK(created == reused);
    CHECK(cache.pipeline_count() == 1);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a state with another blending gets its own pipeline") {
    const ValidatedDevice gpu;
    const SpriteProgram program(gpu.device());
    PipelineCache cache(gpu.device());

    const VkPipeline opaque = cache.pipeline_for(program.state(Blending::disabled));
    const VkPipeline translucent = cache.pipeline_for(program.state(Blending::alpha));

    CHECK(opaque != translucent);
    CHECK(cache.pipeline_count() == 2);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("pipelines built from a shader leave the cache with it and are built again when asked for") {
    const ValidatedDevice gpu;
    const SpriteProgram program(gpu.device());
    PipelineCache cache(gpu.device());
    const GraphicsPipelineState opaque = program.state(Blending::disabled);
    cache.pipeline_for(opaque);
    cache.pipeline_for(program.state(Blending::alpha));

    const std::vector<DeviceHandle<VkPipeline>> taken = cache.take_pipelines_using(opaque.vertex_shader);

    CHECK(taken.size() == 2);
    CHECK(cache.pipeline_count() == 0);
    CHECK(cache.take_pipelines_using(opaque.fragment_shader).empty());
    CHECK(cache.pipeline_for(opaque) != VK_NULL_HANDLE);
    CHECK(cache.pipeline_count() == 1);
    CHECK(gpu.validation_text() == "");
}
