#pragma once

#include <array>
#include <span>
#include <string>
#include <vector>

#include <vulkan/vulkan.h>

#include "descriptors/descriptor_pools.hpp"
#include "draw/batch_geometry.hpp"
#include "draw/batch_renderer.hpp"
#include "draw/material.hpp"
#include "draw/quad_order.hpp"
#include "draw/vertex_format.hpp"
#include "draw/view_constants.hpp"
#include "frame/frame_readback.hpp"
#include "frame/frame_targets.hpp"
#include "pipelines/pipeline_cache.hpp"
#include "support/scene_vertices.hpp"
#include "support/validated_device.hpp"
#include "textures/sampler_cache.hpp"
#include "textures/texture.hpp"
#include "upload/upload_arena.hpp"

namespace minemod::renderer::test_support {

inline constexpr std::array<float, 2> red_texel{0.25f, 0.5f};
inline constexpr std::array<float, 2> green_texel{0.75f, 0.5f};

class BatchStage {
public:
    BatchStage();

    DrawBatch quads(
        const VertexFormat& format,
        std::span<const SceneVertex> corners,
        const Material& material,
        QuadOrder order = QuadOrder::submitted);
    void draw(const DrawBatch& batch);

    FramePixels render();

    VkExtent2D extent() const noexcept {
        return targets_.extent();
    }

    std::string validation_text() const {
        return gpu_.validation_text();
    }

private:
    void upload(const Texture& texture, VkExtent2D extent, std::span<const std::byte> rgba);

    ValidatedDevice gpu_;
    FrameTargets targets_;
    UploadArena uploads_;
    UploadArena geometry_;
    BatchStager stager_;
    Texture surface_;
    Texture lightmap_;
    Texture overlay_;
    SamplerCache samplers_;
    PipelineCache pipelines_;
    DescriptorPools descriptors_;
    ViewConstants view_constants_;
    BatchRenderer renderer_;
    VkDescriptorSet view_ = VK_NULL_HANDLE;
    VkDescriptorSet surface_set_ = VK_NULL_HANDLE;
    std::vector<DrawBatch> batches_;
};

}
