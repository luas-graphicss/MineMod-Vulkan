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
    MINEMOD_OPCODE_DESTROY_BUFFER = 3
};

#define MINEMOD_BUFFER_USAGE_VERTEX (UINT32_C(1) << 0)
#define MINEMOD_BUFFER_USAGE_INDEX (UINT32_C(1) << 1)
#define MINEMOD_BUFFER_USAGE_UNIFORM (UINT32_C(1) << 2)

typedef struct minemod_stream_report {
    uint32_t command_count;
    uint32_t failed_command;
    uint64_t failed_offset;
} minemod_stream_report;

MINEMOD_ABI_BEGIN

minemod_result minemod_validate_command_stream(
    const uint8_t* bytes, uint64_t length, minemod_stream_report* report) MINEMOD_ABI_NOEXCEPT;

MINEMOD_ABI_END

#endif
