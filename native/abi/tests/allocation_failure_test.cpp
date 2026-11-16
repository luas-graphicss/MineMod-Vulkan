#include <doctest.h>

#include <atomic>
#include <cstdint>
#include <cstdlib>
#include <new>

#include "minemod/abi/session.h"
#include "support/stream_builder.hpp"
#include "support/test_session.hpp"

using namespace minemod::abi::testing;

namespace {

std::atomic<std::uint64_t> allocation_count{0};
std::atomic<bool> allocations_fail{false};

class FailingAllocations {
public:
    FailingAllocations() {
        allocations_fail = true;
    }

    ~FailingAllocations() {
        allocations_fail = false;
    }

    FailingAllocations(const FailingAllocations&) = delete;
    FailingAllocations& operator=(const FailingAllocations&) = delete;
};

}

void* operator new(std::size_t size) {
    ++allocation_count;
    if (allocations_fail) {
        throw std::bad_alloc();
    }
    if (void* memory = std::malloc(size == 0 ? 1 : size)) {
        return memory;
    }
    throw std::bad_alloc();
}

void operator delete(void* memory) noexcept {
    std::free(memory);
}

void operator delete(void* memory, std::size_t) noexcept {
    std::free(memory);
}

TEST_CASE("an incompatible ABI is rejected before the first allocation") {
    auto config = default_session_config();
    config.abi_version.major += 1;
    minemod_session* session = nullptr;

    const std::uint64_t before = allocation_count;
    const minemod_result result = minemod_session_create(&config, &session);
    const std::uint64_t after = allocation_count;

    CHECK(result == MINEMOD_RESULT_INCOMPATIBLE_ABI);
    CHECK(session == nullptr);
    CHECK(after == before);
}

TEST_CASE("session creation reports an allocation failure and leaves no session") {
    auto config = default_session_config();
    minemod_session* session = nullptr;
    minemod_result result = MINEMOD_RESULT_OK;
    {
        const FailingAllocations failing;
        result = minemod_session_create(&config, &session);
    }

    CHECK(result == MINEMOD_RESULT_OUT_OF_MEMORY);
    CHECK(session == nullptr);
}

TEST_CASE("a buffer that cannot be allocated aborts its submission and keeps the session usable") {
    TestSession session;
    const auto stream = StreamBuilder().create_buffer(buffer_handle(0), 64, MINEMOD_BUFFER_USAGE_VERTEX).build();
    minemod_stream_report report{};
    minemod_result result = MINEMOD_RESULT_OK;
    {
        const FailingAllocations failing;
        result = minemod_session_submit(session.get(), stream.data(), stream.size(), &report);
    }

    CHECK(result == MINEMOD_RESULT_OUT_OF_MEMORY);
    CHECK(report.failed_command == 0);
    CHECK(session.counters().submissions_aborted == 1);
    CHECK(session.counters().live_buffers == 0);
    CHECK(session.submit(stream) == MINEMOD_RESULT_OK);
    CHECK(session.counters().live_buffer_bytes == 64);
}
