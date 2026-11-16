#include "commands/section_face_commands.hpp"

#include "commands/byte_reader.hpp"
#include "minemod/abi/scene_commands.h"

namespace minemod::scene {

namespace {

static_assert(renderer::section_face_count == MINEMOD_SECTION_FACE_COUNT);
static_assert(renderer::face_index(renderer::SectionFace::west) == MINEMOD_FACE_WEST);
static_assert(renderer::face_index(renderer::SectionFace::east) == MINEMOD_FACE_EAST);
static_assert(renderer::face_index(renderer::SectionFace::down) == MINEMOD_FACE_DOWN);
static_assert(renderer::face_index(renderer::SectionFace::up) == MINEMOD_FACE_UP);
static_assert(renderer::face_index(renderer::SectionFace::north) == MINEMOD_FACE_NORTH);
static_assert(renderer::face_index(renderer::SectionFace::south) == MINEMOD_FACE_SOUTH);

constexpr std::uint64_t known_pairs =
    (std::uint64_t{1} << (MINEMOD_SECTION_FACE_COUNT * MINEMOD_SECTION_FACE_COUNT)) - 1;

constexpr std::uint64_t pair_bit(renderer::SectionFace first, renderer::SectionFace second) noexcept {
    return std::uint64_t{1}
        << (renderer::face_index(first) * MINEMOD_SECTION_FACE_COUNT + renderer::face_index(second));
}

renderer::FaceConnections connections_from(std::uint64_t pairs) noexcept {
    renderer::FaceConnections connections;
    for (const renderer::SectionFace first : renderer::section_faces) {
        for (const renderer::SectionFace second : renderer::section_faces) {
            if ((pairs & pair_bit(first, second)) != 0) {
                connections.connect(first, second);
            }
        }
    }
    return connections;
}

}

Decoded<SectionFaces> decode_section_faces(std::span<const std::byte> payload) {
    ByteReader fields(payload);
    const renderer::SectionPosition section{fields.read_signed(), fields.read_signed(), fields.read_signed()};
    const auto world = fields.read<std::uint32_t>();
    const auto pairs = fields.read<std::uint64_t>();
    if ((pairs & ~known_pairs) != 0) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    return SectionFaces{section, world, connections_from(pairs)};
}

}
