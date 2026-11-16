#include "commands/scene_commands.hpp"

#include <bit>
#include <cstring>
#include <optional>
#include <type_traits>
#include <vector>

#include "commands/byte_reader.hpp"
#include "minemod/abi/scene_commands.h"
#include "pipelines/blending.hpp"
#include "terrain/terrain_vertex.hpp"

namespace minemod::scene {

namespace {

static_assert(std::endian::native == std::endian::little, "scene blobs are copied as little-endian words");
static_assert(sizeof(renderer::TerrainVertex) == MINEMOD_TERRAIN_VERTEX_BYTES);
static_assert(std::is_trivially_copyable_v<renderer::TerrainVertex>);
static_assert(renderer::terrain_layer_count == MINEMOD_TERRAIN_LAYER_COUNT);

constexpr std::uint32_t known_sampling =
    MINEMOD_SAMPLING_LINEAR | MINEMOD_SAMPLING_BLEND_LEVELS | MINEMOD_SAMPLING_REPEAT;
constexpr std::uint32_t known_vertex_elements = MINEMOD_VERTEX_POSITION | MINEMOD_VERTEX_COLOR
    | MINEMOD_VERTEX_TEXTURE | MINEMOD_VERTEX_OVERLAY | MINEMOD_VERTEX_LIGHT | MINEMOD_VERTEX_NORMAL;
constexpr std::uint32_t known_depth = MINEMOD_DEPTH_TEST | MINEMOD_DEPTH_WRITE | MINEMOD_DEPTH_ONLY;
constexpr std::uint32_t known_shading = MINEMOD_SHADING_TEXTURE | MINEMOD_SHADING_LIGHTMAP
    | MINEMOD_SHADING_OVERLAY | MINEMOD_SHADING_NORMAL | MINEMOD_SHADING_FOG | MINEMOD_SHADING_WIDE_LINES;

constexpr std::array vertex_elements{
    renderer::VertexElement::position,
    renderer::VertexElement::color,
    renderer::VertexElement::texture,
    renderer::VertexElement::overlay,
    renderer::VertexElement::light,
    renderer::VertexElement::normal,
};

std::span<const std::byte> blob_after(std::span<const std::byte> payload, std::size_t fixed_bytes) {
    ByteReader length(payload.subspan(fixed_bytes - sizeof(std::uint64_t)));
    return payload.subspan(fixed_bytes, static_cast<std::size_t>(length.read<std::uint64_t>()));
}

std::optional<renderer::FogShape> fog_shape(std::uint32_t code) noexcept {
    switch (code) {
        case MINEMOD_FOG_SPHERE:
            return renderer::FogShape::sphere;
        case MINEMOD_FOG_CYLINDER:
            return renderer::FogShape::cylinder;
        default:
            return std::nullopt;
    }
}

std::optional<VkCompareOp> depth_compare(std::uint32_t code) noexcept {
    switch (code) {
        case MINEMOD_DEPTH_COMPARE_LESS_OR_EQUAL:
            return VK_COMPARE_OP_LESS_OR_EQUAL;
        case MINEMOD_DEPTH_COMPARE_ALWAYS:
            return VK_COMPARE_OP_ALWAYS;
        case MINEMOD_DEPTH_COMPARE_EQUAL:
            return VK_COMPARE_OP_EQUAL;
        case MINEMOD_DEPTH_COMPARE_GREATER:
            return VK_COMPARE_OP_GREATER;
        default:
            return std::nullopt;
    }
}

renderer::Shading shading_from(std::uint32_t bits) noexcept {
    return renderer::Shading{
        .samples_texture = (bits & MINEMOD_SHADING_TEXTURE) != 0,
        .applies_lightmap = (bits & MINEMOD_SHADING_LIGHTMAP) != 0,
        .applies_overlay = (bits & MINEMOD_SHADING_OVERLAY) != 0,
        .shades_by_normal = (bits & MINEMOD_SHADING_NORMAL) != 0,
        .fades_into_fog = (bits & MINEMOD_SHADING_FOG) != 0,
        .widens_lines = (bits & MINEMOD_SHADING_WIDE_LINES) != 0,
    };
}

renderer::VertexFormat format_from(std::uint32_t element_bits) {
    std::array<renderer::VertexElement, renderer::vertex_element_count> listed{};
    std::size_t count = 0;
    for (std::size_t element = 0; element < vertex_elements.size(); ++element) {
        if ((element_bits & (UINT32_C(1) << element)) != 0) {
            listed[count] = vertex_elements[element];
            ++count;
        }
    }
    return renderer::VertexFormat(std::span<const renderer::VertexElement>(listed).first(count));
}

template <typename Item>
std::vector<Item> copy_items(std::span<const std::byte> bytes, std::size_t count) {
    std::vector<Item> items(count);
    if (count != 0) {
        std::memcpy(items.data(), bytes.data(), count * sizeof(Item));
    }
    return items;
}

}

Decoded<TextureDefinition> decode_texture_definition(std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const auto texture = fields.read<std::uint64_t>();
    const VkExtent2D extent{fields.read<std::uint32_t>(), fields.read<std::uint32_t>()};
    const auto mip_levels = fields.read<std::uint32_t>();
    const auto sampling = fields.read<std::uint32_t>();
    if ((sampling & ~known_sampling) != 0) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    return TextureDefinition{texture, extent, mip_levels, sampling};
}

Decoded<TextureWrite> decode_texture_write(std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const auto texture = fields.read<std::uint64_t>();
    const auto level = fields.read<std::uint32_t>();
    const renderer::SpriteRegion region{
        fields.read<std::uint32_t>(),
        fields.read<std::uint32_t>(),
        fields.read<std::uint32_t>(),
        fields.read<std::uint32_t>(),
    };
    if (fields.read<std::uint32_t>() != 0) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    return TextureWrite{texture, level, region, blob_after(payload, MINEMOD_WRITE_TEXTURE_BYTES)};
}

minemod_handle read_texture_handle(std::span<const std::byte> payload) {
    return ByteReader(payload).read<std::uint64_t>();
}

Decoded<SectionPublication> decode_section_publication(std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const renderer::SectionPosition section{fields.read_signed(), fields.read_signed(), fields.read_signed()};
    const auto world = fields.read<std::uint32_t>();
    const std::size_t vertex_count = fields.read<std::uint32_t>();
    const std::size_t index_count = fields.read<std::uint32_t>();
    std::array<renderer::IndexRange, renderer::terrain_layer_count> layers{};
    for (renderer::IndexRange& layer : layers) {
        layer = renderer::IndexRange{fields.read<std::uint32_t>(), fields.read<std::uint32_t>()};
    }
    if (vertex_count > renderer::max_section_vertices || index_count > renderer::max_section_indices) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    const std::span<const std::byte> blob = blob_after(payload, MINEMOD_PUBLISH_SECTION_BYTES);
    const std::size_t vertex_bytes = vertex_count * sizeof(renderer::TerrainVertex);
    if (blob.size() != vertex_bytes + index_count * sizeof(std::uint32_t)) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    return SectionPublication{
        section,
        world,
        renderer::SectionMesh{
            copy_items<renderer::TerrainVertex>(blob, vertex_count),
            copy_items<std::uint32_t>(blob.subspan(vertex_bytes), index_count),
            layers,
        },
    };
}

Decoded<renderer::SectionPosition> decode_section_position(std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const renderer::SectionPosition section{fields.read_signed(), fields.read_signed(), fields.read_signed()};
    if (fields.read<std::uint32_t>() != 0) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    return section;
}

Decoded<FrameStart> decode_frame_start(std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const std::array<float, 4> clear_color = fields.read_floats<4>();
    const auto flags = fields.read<std::uint32_t>();
    const auto reserved = fields.read<std::uint32_t>();
    if ((flags & ~MINEMOD_FRAME_DRAWS_WORLD) != 0 || reserved != 0) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    return FrameStart{clear_color, (flags & MINEMOD_FRAME_DRAWS_WORLD) != 0};
}

Decoded<ViewDefinition> decode_view_definition(std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const auto view = fields.read<std::uint32_t>();
    const std::optional<renderer::FogShape> shape = fog_shape(fields.read<std::uint32_t>());
    const renderer::Matrix4 projection{fields.read_floats<16>()};
    const std::array<float, 4> fog_color = fields.read_floats<4>();
    const float fog_start = fields.read_float();
    const float fog_end = fields.read_float();
    const std::array<float, 3> first_light = fields.read_floats<3>();
    const std::array<float, 3> second_light = fields.read_floats<3>();
    const auto lightmap = fields.read<std::uint64_t>();
    const auto overlay = fields.read<std::uint64_t>();
    if (view >= MINEMOD_FRAME_MAX_VIEWS || !shape) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    const renderer::Fog fog{fog_color, fog_start, fog_end, *shape};
    if (!renderer::has_depth(fog)) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    return ViewDefinition{
        view,
        renderer::SceneView{
            projection,
            fog,
            {
                renderer::Vector3{first_light[0], first_light[1], first_light[2]},
                renderer::Vector3{second_light[0], second_light[1], second_light[2]},
            },
        },
        lightmap,
        overlay,
    };
}

Decoded<TerrainDefinition> decode_terrain_definition(std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const renderer::Matrix4 view_projection{fields.read_floats<16>()};
    const renderer::WorldPosition camera{fields.read_double(), fields.read_double(), fields.read_double()};
    const std::array<float, 4> fog_color = fields.read_floats<4>();
    const float fog_start = fields.read_float();
    const float fog_end = fields.read_float();
    const std::optional<renderer::FogShape> shape = fog_shape(fields.read<std::uint32_t>());
    const auto reserved = fields.read<std::uint32_t>();
    const auto atlas = fields.read<std::uint64_t>();
    const auto lightmap = fields.read<std::uint64_t>();
    if (!shape || reserved != 0) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    const renderer::Fog fog{fog_color, fog_start, fog_end, *shape};
    if (!renderer::has_depth(fog)) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    return TerrainDefinition{view_projection, camera, fog, atlas, lightmap};
}

Decoded<BatchSubmission> decode_batch_submission(std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const auto slot = fields.read<std::uint32_t>();
    const auto view = fields.read<std::uint32_t>();
    const auto elements = fields.read<std::uint32_t>();
    const auto mode = fields.read<std::uint32_t>();
    const auto blending = fields.read<std::uint32_t>();
    const auto depth = fields.read<std::uint32_t>();
    const std::optional<VkCompareOp> compare = depth_compare(fields.read<std::uint32_t>());
    const auto culling = fields.read<std::uint32_t>();
    const auto shading = fields.read<std::uint32_t>();
    const auto program = fields.read<std::uint32_t>();
    const auto texture = fields.read<std::uint64_t>();
    const renderer::Matrix4 model_view{fields.read_floats<16>()};
    const std::array<float, 4> color_modulator = fields.read_floats<4>();
    const float alpha_cutoff = fields.read_float();
    const float line_width = fields.read_float();
    const renderer::DepthBias depth_bias{fields.read_float(), fields.read_float()};

    const bool has_known_elements =
        (elements & MINEMOD_VERTEX_POSITION) != 0 && (elements & ~known_vertex_elements) == 0;
    const bool has_known_state = slot <= MINEMOD_BATCH_INTERFACE && view < MINEMOD_FRAME_MAX_VIEWS
        && mode <= MINEMOD_PRIMITIVE_LINES && blending <= MINEMOD_BLENDING_DARKENING
        && (depth & ~known_depth) == 0 && compare.has_value() && culling <= MINEMOD_CULLING_DISABLED
        && (shading & ~known_shading) == 0;
    if (!has_known_elements || !has_known_state) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    return BatchSubmission{
        slot,
        view,
        program,
        format_from(elements),
        static_cast<renderer::PrimitiveMode>(mode),
        renderer::Material{
            static_cast<renderer::Blending>(blending),
            renderer::DepthState{(depth & MINEMOD_DEPTH_TEST) != 0, (depth & MINEMOD_DEPTH_WRITE) != 0, *compare},
            culling == MINEMOD_CULLING_DISABLED ? renderer::FaceCulling::disabled : renderer::FaceCulling::back_faces,
            alpha_cutoff,
            shading_from(shading),
            depth_bias,
            (depth & MINEMOD_DEPTH_ONLY) == 0,
        },
        texture,
        model_view,
        color_modulator,
        line_width,
        blob_after(payload, MINEMOD_DRAW_BATCH_BYTES),
    };
}

}
