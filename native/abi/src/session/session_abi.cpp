#include "minemod/abi/session.h"

#include <memory>
#include <new>

#include "session/session.hpp"
#include "stream/stream_view.hpp"

static_assert(sizeof(minemod_session_config) == 32);
static_assert(sizeof(minemod_session_counters) == 64);
static_assert(sizeof(minemod_buffer_description) == 24);

struct minemod_session {
    explicit minemod_session(const minemod_session_config& config) noexcept : core(config) {}

    minemod::abi::Session core;
};

namespace {

template <typename Operation>
minemod_result run_guarded(Operation&& operation) noexcept {
    try {
        return operation();
    } catch (const std::bad_alloc&) {
        return MINEMOD_RESULT_OUT_OF_MEMORY;
    } catch (...) {
        return MINEMOD_RESULT_INTERNAL_ERROR;
    }
}

}

minemod_result minemod_session_create(const minemod_session_config* config, minemod_session** session) noexcept {
    if (config == nullptr || session == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    *session = nullptr;
    if (const auto checked = minemod::abi::Session::check_config(*config); checked != MINEMOD_RESULT_OK) {
        return checked;
    }
    return run_guarded([&] {
        *session = std::make_unique<minemod_session>(*config).release();
        return MINEMOD_RESULT_OK;
    });
}

void minemod_session_destroy(minemod_session* session) noexcept {
    std::unique_ptr<minemod_session> owned(session);
}

minemod_result minemod_session_submit(
    minemod_session* session, const uint8_t* bytes, uint64_t length, minemod_stream_report* report) noexcept {
    if (session == nullptr || report == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    minemod::abi::report_rejected_stream(*report);

    std::span<const std::byte> stream;
    if (const auto viewed = minemod::abi::view_stream(bytes, length, stream); viewed != MINEMOD_RESULT_OK) {
        return viewed;
    }
    return run_guarded([&] { return session->core.submit(stream, *report); });
}

minemod_result minemod_session_read_buffer(
    const minemod_session* session,
    minemod_handle buffer,
    uint64_t offset,
    uint8_t* destination,
    uint64_t length) noexcept {
    if (session == nullptr || (destination == nullptr && length != 0)) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    if (length > MINEMOD_SESSION_MAX_BUFFER_BYTES) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    const auto target = std::as_writable_bytes(std::span<uint8_t>(destination, static_cast<std::size_t>(length)));
    return run_guarded([&] { return session->core.read_buffer(buffer, offset, target); });
}

minemod_result minemod_session_describe_buffer(
    const minemod_session* session, minemod_handle buffer, minemod_buffer_description* description) noexcept {
    if (session == nullptr || description == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return run_guarded([&] { return session->core.describe_buffer(buffer, *description); });
}

minemod_result minemod_session_get_counters(
    const minemod_session* session, minemod_session_counters* counters) noexcept {
    if (session == nullptr || counters == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    *counters = session->core.counters();
    return MINEMOD_RESULT_OK;
}
