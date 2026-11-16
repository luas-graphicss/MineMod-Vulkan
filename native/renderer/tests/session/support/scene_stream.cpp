#include "session/support/scene_stream.hpp"

#include <bit>
#include <cstring>

#include "minemod/abi/command_stream.h"
#include "terrain/terrain_vertex.hpp"

namespace minemod::scene::test_support {

namespace {

class Payload {
public:
    Payload& u32(std::uint32_t value) {
        return append(value);
    }

    Payload& u64(std::uint64_t value) {
        return append(value);
    }

    Payload& i32(std::int32_t value) {
        return append(static_cast<std::uint32_t>(value));
    }

    Payload& f32(float value) {
        return append(std::bit_cast<std::uint32_t>(value));
    }

    Payload& f64(double value) {
        return append(std::bit_cast<std::uint64_t>(value));
    }

    template <std::size_t Count>
    Payload& floats(const std::array<float, Count>& values) {
        for (const float value : values) {
            f32(value);
        }
        return *this;
    }

    Payload& fog(const FogState& fog_state) {
        return floats(fog_state.color).f32(fog_state.start).f32(fog_state.end);
    }

    Payload& blob(std::span<const std::uint8_t> bytes) {
        u64(bytes.size());
        bytes_.insert(bytes_.end(), bytes.begin(), bytes.end());
        return *this;
    }

    const std::vector<std::uint8_t>& bytes() const noexcept {
        return bytes_;
    }

private:
    template <typename Value>
    Payload& append(Value value) {
        for (std::size_t index = 0; index < sizeof(Value); ++index) {
            bytes_.push_back(static_cast<std::uint8_t>(value >> (index * 8)));
        }
        return *this;
    }

    std::vector<std::uint8_t> bytes_;
};

std::vector<std::uint8_t> mesh_bytes(const renderer::SectionMesh& mesh) {
    const std::size_t vertex_bytes = mesh.vertices.size() * sizeof(renderer::TerrainVertex);
    const std::size_t index_bytes = mesh.indices.size() * sizeof(std::uint32_t);
    std::vector<std::uint8_t> bytes(vertex_bytes + index_bytes);
    std::memcpy(bytes.data(), mesh.vertices.data(), vertex_bytes);
    std::memcpy(bytes.data() + vertex_bytes, mesh.indices.data(), index_bytes);
    return bytes;
}

}

minemod_handle texture_handle(std::uint32_t index) {
    return minemod_handle_pack(MINEMOD_HANDLE_KIND_TEXTURE, 1, index);
}

SceneStream& SceneStream::define_texture(
    minemod_handle texture, std::uint32_t width, std::uint32_t height, std::uint32_t mip_levels, std::uint32_t sampling) {
    builder_.command(
        MINEMOD_OPCODE_DEFINE_TEXTURE, Payload().u64(texture).u32(width).u32(height).u32(mip_levels).u32(sampling).bytes());
    return *this;
}

SceneStream& SceneStream::write_texture(
    minemod_handle texture, const TextureRegion& region, std::span<const std::uint8_t> texels) {
    Payload payload;
    payload.u64(texture).u32(region.level).u32(region.x).u32(region.y).u32(region.width).u32(region.height).u32(0);
    builder_.command(MINEMOD_OPCODE_WRITE_TEXTURE, payload.blob(texels).bytes());
    return *this;
}

SceneStream& SceneStream::destroy_texture(minemod_handle texture) {
    builder_.command(MINEMOD_OPCODE_DESTROY_TEXTURE, Payload().u64(texture).bytes());
    return *this;
}

SceneStream& SceneStream::publish_section(const SectionKey& section, const renderer::SectionMesh& mesh) {
    Payload payload;
    payload.i32(section.x).i32(section.y).i32(section.z).u32(section.world);
    payload.u32(static_cast<std::uint32_t>(mesh.vertices.size()));
    payload.u32(static_cast<std::uint32_t>(mesh.indices.size()));
    for (const renderer::IndexRange& layer : mesh.layers) {
        payload.u32(layer.first_index).u32(layer.index_count);
    }
    builder_.command(MINEMOD_OPCODE_PUBLISH_SECTION, payload.blob(mesh_bytes(mesh)).bytes());
    return *this;
}

SceneStream& SceneStream::unload_section(const SectionKey& section) {
    builder_.command(
        MINEMOD_OPCODE_UNLOAD_SECTION, Payload().i32(section.x).i32(section.y).i32(section.z).u32(0).bytes());
    return *this;
}

SceneStream& SceneStream::change_world() {
    builder_.command(MINEMOD_OPCODE_CHANGE_WORLD, {});
    return *this;
}

SceneStream& SceneStream::begin_frame(const std::array<float, 4>& clear_color, std::uint32_t flags) {
    builder_.command(MINEMOD_OPCODE_BEGIN_FRAME, Payload().floats(clear_color).u32(flags).u32(0).bytes());
    return *this;
}

SceneStream& SceneStream::set_view(const ViewState& view) {
    Payload payload;
    payload.u32(view.view).u32(view.fog.shape).floats(view.projection).fog(view.fog);
    payload.floats(view.first_light).floats(view.second_light).u64(view.lightmap).u64(view.overlay);
    builder_.command(MINEMOD_OPCODE_SET_VIEW, payload.bytes());
    return *this;
}

SceneStream& SceneStream::set_terrain(const TerrainState& terrain) {
    Payload payload;
    payload.floats(terrain.view_projection);
    for (const double coordinate : terrain.camera) {
        payload.f64(coordinate);
    }
    payload.fog(terrain.fog).u32(terrain.fog.shape).u32(0).u64(terrain.atlas).u64(terrain.lightmap);
    builder_.command(MINEMOD_OPCODE_SET_TERRAIN, payload.bytes());
    return *this;
}

SceneStream& SceneStream::draw_batch(const BatchState& batch, std::span<const std::uint8_t> vertices) {
    Payload payload;
    payload.u32(batch.slot).u32(batch.view).u32(batch.elements).u32(batch.mode).u32(batch.blending);
    payload.u32(batch.depth).u32(batch.depth_compare).u32(batch.culling).u32(batch.shading).u32(0);
    payload.u64(batch.texture).floats(batch.model_view).floats(batch.color_modulator);
    payload.f32(batch.alpha_cutoff).f32(batch.line_width).f32(0.0f).f32(0.0f);
    builder_.command(MINEMOD_OPCODE_DRAW_BATCH, payload.blob(vertices).bytes());
    return *this;
}

SceneStream& SceneStream::end_frame() {
    builder_.command(MINEMOD_OPCODE_END_FRAME, {});
    return *this;
}

SceneStream& SceneStream::set_clip(const ClipState& clip) {
    builder_.command(
        MINEMOD_OPCODE_SET_CLIP, Payload().i32(clip.left).i32(clip.bottom).i32(clip.width).i32(clip.height).bytes());
    return *this;
}

SceneStream& SceneStream::clear_clip() {
    builder_.command(MINEMOD_OPCODE_CLEAR_CLIP, {});
    return *this;
}

SceneStream& SceneStream::set_texture_transform(const std::array<float, 3>& u_row, const std::array<float, 3>& v_row) {
    builder_.command(MINEMOD_OPCODE_SET_TEXTURE_TRANSFORM, Payload().floats(u_row).floats(v_row).bytes());
    return *this;
}

SceneStream& SceneStream::set_quad_order(std::uint32_t order) {
    builder_.command(MINEMOD_OPCODE_SET_QUAD_ORDER, Payload().u32(order).u32(0).bytes());
    return *this;
}

SceneStream& SceneStream::connect_section_faces(const SectionKey& section, std::uint64_t connections) {
    builder_.command(
        MINEMOD_OPCODE_CONNECT_SECTION_FACES,
        Payload().i32(section.x).i32(section.y).i32(section.z).u32(section.world).u64(connections).bytes());
    return *this;
}

}
