#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

#include "minemod/abi/session.h"

namespace minemod::abi::testing {

minemod_session_config default_session_config();

class TestSession {
public:
    explicit TestSession(minemod_session_config config = default_session_config());
    ~TestSession();

    TestSession(const TestSession&) = delete;
    TestSession& operator=(const TestSession&) = delete;

    minemod_session* get() const;
    minemod_result submit(const std::vector<std::uint8_t>& stream, minemod_stream_report& report);
    minemod_result submit(const std::vector<std::uint8_t>& stream);
    std::vector<std::uint8_t> read(minemod_handle buffer, std::uint64_t offset, std::size_t length) const;
    minemod_session_counters counters() const;

private:
    minemod_session* session_ = nullptr;
};

}
