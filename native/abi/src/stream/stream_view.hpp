#pragma once

#include <cstddef>
#include <cstdint>
#include <span>

#include "minemod/abi/command_stream.h"
#include "minemod/abi/result.h"

namespace minemod::abi {

inline minemod_result view_stream(
    const std::uint8_t* bytes, std::uint64_t length, std::span<const std::byte>& stream) noexcept {
    if (bytes == nullptr && length != 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    if (length > MINEMOD_STREAM_MAX_BYTES) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    stream = std::as_bytes(std::span<const std::uint8_t>(bytes, static_cast<std::size_t>(length)));
    return MINEMOD_RESULT_OK;
}

inline void report_rejected_stream(minemod_stream_report& report) noexcept {
    report = minemod_stream_report{.command_count = 0, .failed_command = MINEMOD_NO_COMMAND, .failed_offset = 0};
}

}
