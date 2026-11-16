#ifndef MINEMOD_ABI_COMMAND_STREAM_H
#define MINEMOD_ABI_COMMAND_STREAM_H

#include <stdint.h>

#include "minemod/abi/linkage.h"
#include "minemod/abi/result.h"

#define MINEMOD_STREAM_MAGIC UINT32_C(0x53434D4D)
#define MINEMOD_STREAM_HEADER_BYTES 16
#define MINEMOD_STREAM_MAX_BYTES (UINT32_C(16) * 1024 * 1024)
#define MINEMOD_STREAM_MAX_COMMANDS UINT32_C(65536)

#define MINEMOD_COMMAND_HEADER_BYTES 8
#define MINEMOD_COMMAND_ALIGNMENT 8

#define MINEMOD_NO_COMMAND UINT32_MAX
#define MINEMOD_NO_OFFSET UINT64_MAX

enum {
    MINEMOD_OPCODE_CREATE_BUFFER = 1,
    MINEMOD_OPCODE_WRITE_BUFFER = 2,
    MINEMOD_OPCODE_DESTROY_BUFFER = 3,
    MINEMOD_OPCODE_DEFINE_TEXTURE = 16,
    MINEMOD_OPCODE_WRITE_TEXTURE = 17,
    MINEMOD_OPCODE_DESTROY_TEXTURE = 18,
    MINEMOD_OPCODE_PUBLISH_SECTION = 19,
    MINEMOD_OPCODE_UNLOAD_SECTION = 20,
    MINEMOD_OPCODE_CHANGE_WORLD = 21,
    MINEMOD_OPCODE_BEGIN_FRAME = 22,
    MINEMOD_OPCODE_SET_VIEW = 23,
    MINEMOD_OPCODE_SET_TERRAIN = 24,
    MINEMOD_OPCODE_DRAW_BATCH = 25,
    MINEMOD_OPCODE_END_FRAME = 26,
    MINEMOD_OPCODE_SET_CLIP = 27,
    MINEMOD_OPCODE_CLEAR_CLIP = 28,
    MINEMOD_OPCODE_SET_TEXTURE_TRANSFORM = 29,
    MINEMOD_OPCODE_SET_QUAD_ORDER = 30
};

#define MINEMOD_BUFFER_USAGE_VERTEX (UINT32_C(1) << 0)
#define MINEMOD_BUFFER_USAGE_INDEX (UINT32_C(1) << 1)
#define MINEMOD_BUFFER_USAGE_UNIFORM (UINT32_C(1) << 2)

typedef struct minemod_stream_report {
    uint32_t command_count;
    uint32_t failed_command;
    uint64_t failed_offset;
} minemod_stream_report;

typedef struct minemod_command_record {
    uint16_t opcode;
    uint16_t reserved;
    uint32_t payload_bytes;
    const uint8_t* payload;
} minemod_command_record;

typedef minemod_result (*minemod_command_visitor)(void* context, const minemod_command_record* record);

MINEMOD_ABI_BEGIN

minemod_result minemod_validate_command_stream(
    const uint8_t* bytes, uint64_t length, minemod_stream_report* report) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_visit_command_stream(
    const uint8_t* bytes,
    uint64_t length,
    minemod_command_visitor visitor,
    void* context,
    minemod_stream_report* report) MINEMOD_ABI_NOEXCEPT;

MINEMOD_ABI_END

#endif
