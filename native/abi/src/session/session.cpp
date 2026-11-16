#include "session/session.hpp"

#include <new>

#include "negotiation/abi_compatibility.hpp"

namespace minemod::abi {

minemod_result Session::check_config(const minemod_session_config& config) noexcept {
    if (config.struct_bytes != sizeof(minemod_session_config) || config.reserved != 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    if (!accepts_client(config.abi_version)) {
        return MINEMOD_RESULT_INCOMPATIBLE_ABI;
    }
    if ((config.required_capabilities & ~supported_capabilities) != 0) {
        return MINEMOD_RESULT_UNSUPPORTED_CAPABILITY;
    }
    if (config.max_buffers == 0 || config.max_buffer_bytes == 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    if (config.max_buffers > MINEMOD_SESSION_MAX_BUFFERS
        || config.max_buffer_bytes > MINEMOD_SESSION_MAX_BUFFER_BYTES) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    return MINEMOD_RESULT_OK;
}

Session::Session(const minemod_session_config& config) noexcept
    : buffers_(config.max_buffers, config.max_buffer_bytes) {}

minemod_result Session::submit(std::span<const std::byte> stream, minemod_stream_report& report) {
    if (const auto validation = validate_command_stream(stream, report); validation != MINEMOD_RESULT_OK) {
        ++submissions_rejected_;
        return validation;
    }

    executing_submission_ = submissions_accepted_ + submissions_aborted_ + 1;
    const auto execution = walk_command_stream(stream, *this, report);
    commands_executed_ += report.command_count;
    if (execution == MINEMOD_RESULT_OK) {
        ++submissions_accepted_;
    } else {
        ++submissions_aborted_;
    }
    return execution;
}

minemod_result Session::read_buffer(
    minemod_handle buffer, std::uint64_t offset, std::span<std::byte> destination) const {
    return buffers_.read(buffer, offset, destination);
}

minemod_result Session::describe_buffer(minemod_handle buffer, minemod_buffer_description& description) const {
    return buffers_.describe(buffer, description);
}

minemod_session_counters Session::counters() const noexcept {
    const BufferCounters& buffers = buffers_.counters();
    return minemod_session_counters{
        .submissions_accepted = submissions_accepted_,
        .submissions_rejected = submissions_rejected_,
        .submissions_aborted = submissions_aborted_,
        .commands_executed = commands_executed_,
        .buffers_created = buffers.created,
        .buffers_destroyed = buffers.destroyed,
        .live_buffers = buffers.live,
        .live_buffer_bytes = buffers.live_bytes,
    };
}

minemod_result Session::create_buffer(const CreateBufferCommand& command) {
    try {
        return buffers_.create(command.handle, command.size_bytes, command.usage, executing_submission_);
    } catch (const std::bad_alloc&) {
        return MINEMOD_RESULT_OUT_OF_MEMORY;
    }
}

minemod_result Session::write_buffer(const WriteBufferCommand& command) {
    return buffers_.write(command.handle, command.offset, command.data, executing_submission_);
}

minemod_result Session::destroy_buffer(const DestroyBufferCommand& command) {
    return buffers_.destroy(command.handle);
}

minemod_result Session::scene_command(const SceneCommand&) {
    return MINEMOD_RESULT_UNSUPPORTED_CAPABILITY;
}

}
