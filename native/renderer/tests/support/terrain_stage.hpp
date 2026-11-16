#pragma once

#include <cstdint>
#include <optional>
#include <span>
#include <string>

#include <vulkan/vulkan.h>

#include "descriptors/descriptor_pools.hpp"
#include "device/device.hpp"
#include "frame/frame_readback.hpp"
#include "frame/frame_targets.hpp"
#include "lifetime/retirement_queue.hpp"
#include "math/vector.hpp"
#include "pipelines/pipeline_cache.hpp"
#include "support/validated_device.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/terrain_renderer.hpp"
#include "terrain/terrain_sections.hpp"
#include "textures/sampler_cache.hpp"
#include "textures/texture.hpp"
#include "upload/upload_arena.hpp"
#include "work/job_epochs.hpp"

namespace minemod::renderer::test_support {

struct TerrainView {
    WorldPosition camera;
    std::optional<std::span<const SectionPosition>> reachable_sections{};
};

class TerrainStage {
public:
    TerrainStage();

    const Device& device() const noexcept {
        return gpu_.device();
    }

    UploadArena& arena() noexcept {
        return arena_;
    }

    const TerrainSections& sections() const noexcept {
        return sections_;
    }

    void publish(const SectionPosition& section, const SectionMesh& mesh);
    FramePixels render(const TerrainView& view);

    std::uint32_t draw_count() const noexcept {
        return draw_count_;
    }

    std::string validation_text() const {
        return gpu_.validation_text();
    }

private:
    void upload_white(const Texture& texture, VkExtent2D extent);

    ValidatedDevice gpu_;
    FrameTargets targets_;
    UploadArena arena_;
    Texture atlas_;
    Texture lightmap_;
    SamplerCache samplers_;
    PipelineCache pipelines_;
    DescriptorPools descriptors_;
    RetirementQueue retirements_;
    JobEpochs epochs_;
    TerrainSections sections_;
    TerrainRenderer renderer_;
    VkDescriptorSet textures_ = VK_NULL_HANDLE;
    std::uint32_t draw_count_ = 0;
};

}
