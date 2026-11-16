#include "commands/batch_option_commands.hpp"

#include <optional>

#include "commands/byte_reader.hpp"
#include "interface/clip_rectangle.hpp"
#include "minemod/abi/command_stream.h"
#include "minemod/abi/scene_commands.h"

namespace minemod::scene {

namespace {

std::optional<renderer::QuadOrder> quad_order(std::uint32_t code) noexcept {
    switch (code) {
        case MINEMOD_QUAD_ORDER_SUBMITTED:
            return renderer::QuadOrder::submitted;
        case MINEMOD_QUAD_ORDER_FARTHEST_FIRST:
            return renderer::QuadOrder::farthest_first;
        case MINEMOD_QUAD_ORDER_RISING_DEPTH:
            return renderer::QuadOrder::rising_depth;
        default:
            return std::nullopt;
    }
}

minemod_result set_clip(BatchOptions& options, VkExtent2D frame, std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const renderer::LowerLeftRectangle rectangle{
        fields.read_signed(), fields.read_signed(), fields.read_signed(), fields.read_signed()};
    if (rectangle.width < 0 || rectangle.height < 0) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    options.clip = renderer::clip_rectangle(rectangle, frame);
    return MINEMOD_RESULT_OK;
}

minemod_result set_texture_transform(BatchOptions& options, std::span<const std::byte> payload) {
    ByteReader fields(payload);
    options.texture_transform = renderer::TextureTransform{fields.read_floats<3>(), fields.read_floats<3>()};
    return MINEMOD_RESULT_OK;
}

minemod_result set_quad_order(BatchOptions& options, std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const std::optional<renderer::QuadOrder> order = quad_order(fields.read<std::uint32_t>());
    const auto reserved = fields.read<std::uint32_t>();
    if (!order || reserved != 0) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    options.quad_order = *order;
    return MINEMOD_RESULT_OK;
}

}

minemod_result change_batch_options(
    BatchOptions& options, VkExtent2D frame, std::uint16_t opcode, std::span<const std::byte> payload) {
    switch (opcode) {
        case MINEMOD_OPCODE_SET_CLIP:
            return set_clip(options, frame, payload);
        case MINEMOD_OPCODE_CLEAR_CLIP:
            options.clip.reset();
            return MINEMOD_RESULT_OK;
        case MINEMOD_OPCODE_SET_TEXTURE_TRANSFORM:
            return set_texture_transform(options, payload);
        case MINEMOD_OPCODE_SET_QUAD_ORDER:
            return set_quad_order(options, payload);
        default:
            return MINEMOD_RESULT_UNSUPPORTED_CAPABILITY;
    }
}

}
