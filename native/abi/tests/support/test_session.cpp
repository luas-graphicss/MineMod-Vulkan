#include "support/test_session.hpp"

#include <stdexcept>
#include <string>

namespace minemod::abi::testing {

minemod_session_config default_session_config() {
    return minemod_session_config{
        .struct_bytes = sizeof(minemod_session_config),
        .abi_version = {MINEMOD_ABI_VERSION_MAJOR, MINEMOD_ABI_VERSION_MINOR},
        .required_capabilities = MINEMOD_CAPABILITY_COMMAND_STREAM | MINEMOD_CAPABILITY_BUFFER_RESOURCES,
        .max_buffers = 64,
        .reserved = 0,
        .max_buffer_bytes = 4096,
    };
}

TestSession::TestSession(minemod_session_config config) {
    const minemod_result created = minemod_session_create(&config, &session_);
    if (created != MINEMOD_RESULT_OK) {
        throw std::runtime_error(std::string("session creation failed: ") + minemod_result_name(created));
    }
}

TestSession::~TestSession() {
    minemod_session_destroy(session_);
}

minemod_session* TestSession::get() const {
    return session_;
}

minemod_result TestSession::submit(const std::vector<std::uint8_t>& stream, minemod_stream_report& report) {
    return minemod_session_submit(session_, stream.data(), stream.size(), &report);
}

minemod_result TestSession::submit(const std::vector<std::uint8_t>& stream) {
    minemod_stream_report report{};
    return submit(stream, report);
}

std::vector<std::uint8_t> TestSession::read(minemod_handle buffer, std::uint64_t offset, std::size_t length) const {
    std::vector<std::uint8_t> bytes(length);
    const minemod_result result = minemod_session_read_buffer(session_, buffer, offset, bytes.data(), length);
    if (result != MINEMOD_RESULT_OK) {
        throw std::runtime_error(std::string("buffer read failed: ") + minemod_result_name(result));
    }
    return bytes;
}

minemod_session_counters TestSession::counters() const {
    minemod_session_counters counters{};
    if (minemod_session_get_counters(session_, &counters) != MINEMOD_RESULT_OK) {
        throw std::runtime_error("session counters are unavailable");
    }
    return counters;
}

}
