#pragma once

#include <algorithm>
#include <array>
#include <cstdint>

#include "minemod/abi/command_stream.h"
#include "minemod/abi/scene_commands.h"

namespace minemod::abi {

enum class RecordTail : std::uint8_t { none, blob };

struct SceneRecordShape {
    std::uint16_t opcode;
    std::uint32_t fixed_bytes;
    RecordTail tail;
};

inline constexpr std::array scene_record_shapes{
    SceneRecordShape{MINEMOD_OPCODE_DEFINE_TEXTURE, MINEMOD_DEFINE_TEXTURE_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_WRITE_TEXTURE, MINEMOD_WRITE_TEXTURE_BYTES, RecordTail::blob},
    SceneRecordShape{MINEMOD_OPCODE_DESTROY_TEXTURE, MINEMOD_DESTROY_TEXTURE_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_PUBLISH_SECTION, MINEMOD_PUBLISH_SECTION_BYTES, RecordTail::blob},
    SceneRecordShape{MINEMOD_OPCODE_UNLOAD_SECTION, MINEMOD_UNLOAD_SECTION_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_CHANGE_WORLD, MINEMOD_CHANGE_WORLD_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_BEGIN_FRAME, MINEMOD_BEGIN_FRAME_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_SET_VIEW, MINEMOD_SET_VIEW_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_SET_TERRAIN, MINEMOD_SET_TERRAIN_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_DRAW_BATCH, MINEMOD_DRAW_BATCH_BYTES, RecordTail::blob},
    SceneRecordShape{MINEMOD_OPCODE_END_FRAME, MINEMOD_END_FRAME_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_SET_CLIP, MINEMOD_SET_CLIP_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_CLEAR_CLIP, MINEMOD_CLEAR_CLIP_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_SET_TEXTURE_TRANSFORM, MINEMOD_SET_TEXTURE_TRANSFORM_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_SET_QUAD_ORDER, MINEMOD_SET_QUAD_ORDER_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_CONNECT_SECTION_FACES, MINEMOD_CONNECT_SECTION_FACES_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_CLEAR_DEPTH, MINEMOD_CLEAR_DEPTH_BYTES, RecordTail::none},
    SceneRecordShape{MINEMOD_OPCODE_BLUR_FRAME, MINEMOD_BLUR_FRAME_BYTES, RecordTail::none},
};

constexpr const SceneRecordShape* find_scene_record_shape(std::uint16_t opcode) noexcept {
    const auto shape = std::ranges::find(scene_record_shapes, opcode, &SceneRecordShape::opcode);
    return shape == scene_record_shapes.end() ? nullptr : &*shape;
}

}
