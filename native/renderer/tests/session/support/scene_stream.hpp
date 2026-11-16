#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

#include "minemod/abi/handle.h"
#include "minemod/abi/scene_commands.h"
#include "support/stream_builder.hpp"
#include "terrain/section_mesh.hpp"

namespace minemod::scene::test_support {

inline constexpr std::array<float, 16> identity{
    1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f,
};

struct FogState {
    std::array<float, 4> color{0.0f, 0.0f, 0.0f, 1.0f};
    float start = 1000.0f;
    float end = 2000.0f;
    std::uint32_t shape = MINEMOD_FOG_SPHERE;
};

struct ViewState {
    std::uint32_t view = 0;
    std::array<float, 16> projection = identity;
    FogState fog{};
    std::array<float, 3> first_light{0.0f, 1.0f, 0.0f};
    std::array<float, 3> second_light{0.0f, 0.0f, 0.0f};
    minemod_handle lightmap = MINEMOD_HANDLE_NULL;
    minemod_handle overlay = MINEMOD_HANDLE_NULL;
};

struct TerrainState {
    std::array<float, 16> view_projection = identity;
    std::array<double, 3> camera{0.0, 0.0, 0.0};
    FogState fog{};
    minemod_handle atlas = MINEMOD_HANDLE_NULL;
    minemod_handle lightmap = MINEMOD_HANDLE_NULL;
};

struct BatchState {
    std::uint32_t slot = MINEMOD_BATCH_INTERFACE;
    std::uint32_t view = 0;
    std::uint32_t elements = MINEMOD_VERTEX_POSITION | MINEMOD_VERTEX_COLOR;
    std::uint32_t mode = MINEMOD_PRIMITIVE_QUADS;
    std::uint32_t blending = MINEMOD_BLENDING_DISABLED;
    std::uint32_t depth = 0;
    std::uint32_t depth_compare = MINEMOD_DEPTH_COMPARE_ALWAYS;
    std::uint32_t culling = MINEMOD_CULLING_DISABLED;
    std::uint32_t shading = 0;
    std::uint32_t program = 0;
    minemod_handle texture = MINEMOD_HANDLE_NULL;
    std::array<float, 16> model_view = identity;
    std::array<float, 4> color_modulator{1.0f, 1.0f, 1.0f, 1.0f};
    float alpha_cutoff = 0.0f;
    float line_width = 1.0f;
};

struct TextureRegion {
    std::uint32_t level;
    std::uint32_t x;
    std::uint32_t y;
    std::uint32_t width;
    std::uint32_t height;
};

struct ClipState {
    std::int32_t left;
    std::int32_t bottom;
    std::int32_t width;
    std::int32_t height;
};

struct SectionKey {
    std::int32_t x;
    std::int32_t y;
    std::int32_t z;
    std::uint32_t world;
};

minemod_handle texture_handle(std::uint32_t index);

class SceneStream {
public:
    SceneStream& define_texture(
        minemod_handle texture, std::uint32_t width, std::uint32_t height, std::uint32_t mip_levels, std::uint32_t sampling);
    SceneStream& write_texture(
        minemod_handle texture, const TextureRegion& region, std::span<const std::uint8_t> texels);
    SceneStream& destroy_texture(minemod_handle texture);
    SceneStream& publish_section(const SectionKey& section, const renderer::SectionMesh& mesh);
    SceneStream& unload_section(const SectionKey& section);
    SceneStream& change_world();
    SceneStream& begin_frame(const std::array<float, 4>& clear_color, std::uint32_t flags);
    SceneStream& set_view(const ViewState& view);
    SceneStream& set_terrain(const TerrainState& terrain);
    SceneStream& draw_batch(const BatchState& batch, std::span<const std::uint8_t> vertices);
    SceneStream& end_frame();
    SceneStream& set_clip(const ClipState& clip);
    SceneStream& clear_clip();
    SceneStream& set_texture_transform(const std::array<float, 3>& u_row, const std::array<float, 3>& v_row);
    SceneStream& set_quad_order(std::uint32_t order);
    SceneStream& connect_section_faces(const SectionKey& section, std::uint64_t connections);

    std::vector<std::uint8_t> build() const {
        return builder_.build();
    }

private:
    abi::testing::StreamBuilder builder_;
};

}
