#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <span>
#include <utility>
#include <variant>

#include <vulkan/vulkan.h>

#include "draw/material.hpp"
#include "draw/primitive_mode.hpp"
#include "draw/vertex_format.hpp"
#include "draw/view_constants.hpp"
#include "effects/frame_blur.hpp"
#include "environment/fog.hpp"
#include "math/matrix.hpp"
#include "math/vector.hpp"
#include "minemod/abi/handle.h"
#include "minemod/abi/result.h"
#include "terrain/section_mesh.hpp"
#include "textures/atlas_layout.hpp"
#include "work/job_epochs.hpp"

namespace minemod::scene {

struct TextureDefinition {
    minemod_handle texture;
    VkExtent2D extent;
    std::uint32_t mip_levels;
    std::uint32_t sampling;
};

struct TextureWrite {
    minemod_handle texture;
    std::uint32_t level;
    renderer::SpriteRegion region;
    std::span<const std::byte> texels;
};

struct SectionPublication {
    renderer::SectionPosition section;
    std::uint32_t world;
    renderer::SectionMesh mesh;
};

struct FrameStart {
    std::array<float, 4> clear_color;
    bool draws_world;
};

struct ViewDefinition {
    std::uint32_t view;
    renderer::SceneView scene;
    minemod_handle lightmap;
    minemod_handle overlay;
};

struct TerrainDefinition {
    renderer::Matrix4 view_projection;
    renderer::WorldPosition camera;
    renderer::Fog fog;
    minemod_handle atlas;
    minemod_handle lightmap;
};

struct BatchSubmission {
    std::uint32_t slot;
    std::uint32_t view;
    std::uint32_t program;
    renderer::VertexFormat format;
    renderer::PrimitiveMode mode;
    renderer::Material material;
    minemod_handle texture;
    renderer::Matrix4 model_view;
    std::array<float, 4> color_modulator;
    float line_width;
    std::span<const std::byte> vertices;
};

template <typename Command>
class Decoded {
public:
    Decoded(Command command) : outcome_(std::move(command)) {}
    Decoded(minemod_result refusal) : outcome_(refusal) {}

    minemod_result result() const noexcept {
        const minemod_result* refusal = std::get_if<minemod_result>(&outcome_);
        return refusal == nullptr ? MINEMOD_RESULT_OK : *refusal;
    }

    const Command& command() const {
        return std::get<Command>(outcome_);
    }

private:
    std::variant<Command, minemod_result> outcome_;
};

Decoded<TextureDefinition> decode_texture_definition(std::span<const std::byte> payload);
Decoded<TextureWrite> decode_texture_write(std::span<const std::byte> payload);
minemod_handle read_texture_handle(std::span<const std::byte> payload);
Decoded<SectionPublication> decode_section_publication(std::span<const std::byte> payload);
Decoded<renderer::SectionPosition> decode_section_position(std::span<const std::byte> payload);
Decoded<FrameStart> decode_frame_start(std::span<const std::byte> payload);
Decoded<ViewDefinition> decode_view_definition(std::span<const std::byte> payload);
Decoded<TerrainDefinition> decode_terrain_definition(std::span<const std::byte> payload);
Decoded<BatchSubmission> decode_batch_submission(std::span<const std::byte> payload);
Decoded<renderer::FrameBlur> decode_frame_blur(std::span<const std::byte> payload);

}
