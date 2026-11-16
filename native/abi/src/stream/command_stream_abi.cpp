#include "minemod/abi/command_stream.h"

#include "stream/command_stream_walker.hpp"
#include "stream/stream_view.hpp"

static_assert(sizeof(minemod_stream_report) == 16);

minemod_result minemod_validate_command_stream(
    const uint8_t* bytes, uint64_t length, minemod_stream_report* report) noexcept {
    if (report == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    minemod::abi::report_rejected_stream(*report);

    std::span<const std::byte> stream;
    if (const auto viewed = minemod::abi::view_stream(bytes, length, stream); viewed != MINEMOD_RESULT_OK) {
        return viewed;
    }
    try {
        return minemod::abi::validate_command_stream(stream, *report);
    } catch (...) {
        return MINEMOD_RESULT_INTERNAL_ERROR;
    }
}
