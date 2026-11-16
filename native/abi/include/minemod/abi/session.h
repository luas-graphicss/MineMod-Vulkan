#ifndef MINEMOD_ABI_SESSION_H
#define MINEMOD_ABI_SESSION_H

#include <stdint.h>

#include "minemod/abi/capabilities.h"
#include "minemod/abi/command_stream.h"
#include "minemod/abi/handle.h"
#include "minemod/abi/linkage.h"
#include "minemod/abi/result.h"
#include "minemod/abi/version.h"

#define MINEMOD_SESSION_MAX_BUFFERS (UINT32_C(1) << 20)
#define MINEMOD_SESSION_MAX_BUFFER_BYTES (UINT64_C(1) << 30)

typedef struct minemod_session minemod_session;

typedef struct minemod_session_config {
    uint32_t struct_bytes;
    minemod_abi_version abi_version;
    minemod_capabilities required_capabilities;
    uint32_t max_buffers;
    uint32_t reserved;
    uint64_t max_buffer_bytes;
} minemod_session_config;

typedef struct minemod_session_counters {
    uint64_t submissions_accepted;
    uint64_t submissions_rejected;
    uint64_t submissions_aborted;
    uint64_t commands_executed;
    uint64_t buffers_created;
    uint64_t buffers_destroyed;
    uint64_t live_buffers;
    uint64_t live_buffer_bytes;
} minemod_session_counters;

typedef struct minemod_buffer_description {
    uint64_t size_bytes;
    uint64_t last_used_submission;
    uint32_t usage;
    uint32_t generation;
} minemod_buffer_description;

MINEMOD_ABI_BEGIN

minemod_result minemod_session_create(
    const minemod_session_config* config, minemod_session** session) MINEMOD_ABI_NOEXCEPT;

void minemod_session_destroy(minemod_session* session) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_session_submit(
    minemod_session* session,
    const uint8_t* bytes,
    uint64_t length,
    minemod_stream_report* report) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_session_read_buffer(
    const minemod_session* session,
    minemod_handle buffer,
    uint64_t offset,
    uint8_t* destination,
    uint64_t length) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_session_describe_buffer(
    const minemod_session* session,
    minemod_handle buffer,
    minemod_buffer_description* description) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_session_get_counters(
    const minemod_session* session, minemod_session_counters* counters) MINEMOD_ABI_NOEXCEPT;

MINEMOD_ABI_END

#endif
