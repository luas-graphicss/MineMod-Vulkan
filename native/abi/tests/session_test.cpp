#include <doctest.h>

#include <cstdint>
#include <vector>

#include "minemod/abi/session.h"
#include "support/stream_builder.hpp"
#include "support/test_session.hpp"

using namespace minemod::abi::testing;

namespace {

const std::vector<std::uint8_t> sample_data = {10, 20, 30, 40, 50};

minemod_result create_with(minemod_session_config config) {
    minemod_session* session = nullptr;
    const minemod_result result = minemod_session_create(&config, &session);
    const bool left_no_session = session == nullptr;
    minemod_session_destroy(session);
    return result == MINEMOD_RESULT_OK || left_no_session ? result : MINEMOD_RESULT_INTERNAL_ERROR;
}

}

TEST_CASE("a written buffer reads back from session-owned memory") {
    TestSession session;
    auto stream = StreamBuilder()
                      .create_buffer(buffer_handle(3), 16, MINEMOD_BUFFER_USAGE_VERTEX)
                      .write_buffer(buffer_handle(3), 4, sample_data)
                      .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);
    stream.assign(stream.size(), 0xEE);

    CHECK(session.read(buffer_handle(3), 4, sample_data.size()) == sample_data);
    CHECK(session.read(buffer_handle(3), 0, 4) == std::vector<std::uint8_t>(4, 0));
}

TEST_CASE("a buffer records its size, usage, generation and last submission") {
    TestSession session;
    const minemod_handle buffer = buffer_handle(1, 7);
    REQUIRE(session.submit(StreamBuilder()
                               .create_buffer(buffer, 32, MINEMOD_BUFFER_USAGE_INDEX | MINEMOD_BUFFER_USAGE_VERTEX)
                               .build())
            == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(StreamBuilder().build()) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(StreamBuilder().write_buffer(buffer, 0, sample_data).build()) == MINEMOD_RESULT_OK);

    minemod_buffer_description description{};
    REQUIRE(minemod_session_describe_buffer(session.get(), buffer, &description) == MINEMOD_RESULT_OK);

    CHECK(description.size_bytes == 32);
    CHECK(description.usage == (MINEMOD_BUFFER_USAGE_INDEX | MINEMOD_BUFFER_USAGE_VERTEX));
    CHECK(description.generation == 7);
    CHECK(description.last_used_submission == 3);
}

TEST_CASE("a destroyed buffer leaves a stale handle behind") {
    TestSession session;
    const minemod_handle buffer = buffer_handle(0);
    REQUIRE(session.submit(StreamBuilder()
                               .create_buffer(buffer, 8, MINEMOD_BUFFER_USAGE_UNIFORM)
                               .destroy_buffer(buffer)
                               .build())
            == MINEMOD_RESULT_OK);

    std::uint8_t byte = 0;
    CHECK(minemod_session_read_buffer(session.get(), buffer, 0, &byte, 1) == MINEMOD_RESULT_STALE_HANDLE);
    CHECK(session.submit(StreamBuilder().write_buffer(buffer, 0, sample_data).build())
          == MINEMOD_RESULT_STALE_HANDLE);
    CHECK(session.submit(StreamBuilder().destroy_buffer(buffer).build()) == MINEMOD_RESULT_STALE_HANDLE);
}

TEST_CASE("a recycled slot only answers to its newest generation") {
    TestSession session;
    const minemod_handle first = buffer_handle(5, 1);
    const minemod_handle second = buffer_handle(5, 2);
    REQUIRE(session.submit(StreamBuilder()
                               .create_buffer(first, 8, MINEMOD_BUFFER_USAGE_VERTEX)
                               .destroy_buffer(first)
                               .create_buffer(second, 8, MINEMOD_BUFFER_USAGE_VERTEX)
                               .write_buffer(second, 0, sample_data)
                               .build())
            == MINEMOD_RESULT_OK);

    std::uint8_t byte = 0;
    CHECK(minemod_session_read_buffer(session.get(), first, 0, &byte, 1) == MINEMOD_RESULT_STALE_HANDLE);
    CHECK(session.read(second, 0, sample_data.size()) == sample_data);
}

TEST_CASE("a slot cannot go back to a generation it already used") {
    TestSession session;
    const minemod_handle buffer = buffer_handle(2, 4);
    REQUIRE(session.submit(StreamBuilder()
                               .create_buffer(buffer, 8, MINEMOD_BUFFER_USAGE_VERTEX)
                               .destroy_buffer(buffer)
                               .build())
            == MINEMOD_RESULT_OK);

    CHECK(session.submit(StreamBuilder().create_buffer(buffer, 8, MINEMOD_BUFFER_USAGE_VERTEX).build())
          == MINEMOD_RESULT_STALE_HANDLE);
    CHECK(session.submit(StreamBuilder().create_buffer(buffer_handle(2, 3), 8, MINEMOD_BUFFER_USAGE_VERTEX).build())
          == MINEMOD_RESULT_STALE_HANDLE);
}

TEST_CASE("handles that never named a buffer are invalid") {
    TestSession session;
    REQUIRE(session.submit(StreamBuilder().create_buffer(buffer_handle(0), 8, MINEMOD_BUFFER_USAGE_VERTEX).build())
            == MINEMOD_RESULT_OK);

    SUBCASE("null handle") {
        CHECK(session.submit(StreamBuilder().destroy_buffer(MINEMOD_HANDLE_NULL).build())
              == MINEMOD_RESULT_INVALID_HANDLE);
    }
    SUBCASE("slot never created") {
        CHECK(session.submit(StreamBuilder().destroy_buffer(buffer_handle(9)).build())
              == MINEMOD_RESULT_INVALID_HANDLE);
    }
    SUBCASE("generation from the future") {
        CHECK(session.submit(StreamBuilder().destroy_buffer(buffer_handle(0, 2)).build())
              == MINEMOD_RESULT_INVALID_HANDLE);
    }
    SUBCASE("another resource kind") {
        const minemod_handle foreign = minemod_handle_pack(MINEMOD_HANDLE_KIND_BUFFER + 1, 1, 0);
        CHECK(session.submit(StreamBuilder().destroy_buffer(foreign).build()) == MINEMOD_RESULT_INVALID_HANDLE);
    }
    SUBCASE("slot beyond the session limit") {
        const minemod_handle beyond = buffer_handle(default_session_config().max_buffers);
        CHECK(session.submit(StreamBuilder().create_buffer(beyond, 8, MINEMOD_BUFFER_USAGE_VERTEX).build())
              == MINEMOD_RESULT_INVALID_HANDLE);
    }
    SUBCASE("live slot claimed by a newer generation") {
        CHECK(session.submit(
                  StreamBuilder().create_buffer(buffer_handle(0, 2), 8, MINEMOD_BUFFER_USAGE_VERTEX).build())
              == MINEMOD_RESULT_INVALID_HANDLE);
    }
}

TEST_CASE("reads and writes outside a buffer are rejected") {
    TestSession session;
    const minemod_handle buffer = buffer_handle(0);
    REQUIRE(session.submit(StreamBuilder().create_buffer(buffer, 8, MINEMOD_BUFFER_USAGE_VERTEX).build())
            == MINEMOD_RESULT_OK);

    std::uint8_t bytes[16] = {};
    CHECK(session.submit(StreamBuilder().write_buffer(buffer, 4, sample_data).build())
          == MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS);
    CHECK(minemod_session_read_buffer(session.get(), buffer, 0, bytes, 9) == MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS);
    CHECK(minemod_session_read_buffer(session.get(), buffer, 9, bytes, 0) == MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS);
    CHECK(minemod_session_read_buffer(session.get(), buffer, 0, bytes, UINT64_MAX)
          == MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS);
}

TEST_CASE("the buffer byte budget is enforced and released on destruction") {
    auto config = default_session_config();
    config.max_buffer_bytes = 24;
    TestSession session(config);
    REQUIRE(session.submit(StreamBuilder().create_buffer(buffer_handle(0), 16, MINEMOD_BUFFER_USAGE_VERTEX).build())
            == MINEMOD_RESULT_OK);

    CHECK(session.submit(StreamBuilder().create_buffer(buffer_handle(1), 16, MINEMOD_BUFFER_USAGE_VERTEX).build())
          == MINEMOD_RESULT_LIMIT_EXCEEDED);
    CHECK(session.submit(StreamBuilder()
                             .destroy_buffer(buffer_handle(0))
                             .create_buffer(buffer_handle(1), 24, MINEMOD_BUFFER_USAGE_VERTEX)
                             .build())
          == MINEMOD_RESULT_OK);
    CHECK(session.counters().live_buffer_bytes == 24);
}

TEST_CASE("a failing command aborts the submission after the commands before it") {
    TestSession session;
    minemod_stream_report report{};

    const minemod_result result = session.submit(
        StreamBuilder()
            .create_buffer(buffer_handle(0), 8, MINEMOD_BUFFER_USAGE_VERTEX)
            .destroy_buffer(buffer_handle(7))
            .create_buffer(buffer_handle(1), 8, MINEMOD_BUFFER_USAGE_VERTEX)
            .build(),
        report);

    CHECK(result == MINEMOD_RESULT_INVALID_HANDLE);
    CHECK(report.command_count == 1);
    CHECK(report.failed_command == 1);
    CHECK(report.failed_offset == MINEMOD_STREAM_HEADER_BYTES + 32);

    const auto counters = session.counters();
    CHECK(counters.submissions_aborted == 1);
    CHECK(counters.submissions_accepted == 0);
    CHECK(counters.commands_executed == 1);
    CHECK(counters.live_buffers == 1);
}

TEST_CASE("a structurally invalid stream changes nothing") {
    TestSession session;
    auto stream = StreamBuilder()
                      .create_buffer(buffer_handle(0), 8, MINEMOD_BUFFER_USAGE_VERTEX)
                      .command(0x7FFF, {})
                      .build();

    CHECK(session.submit(stream) == MINEMOD_RESULT_UNKNOWN_OPCODE);

    const auto counters = session.counters();
    CHECK(counters.submissions_rejected == 1);
    CHECK(counters.commands_executed == 0);
    CHECK(counters.buffers_created == 0);
    CHECK(counters.live_buffers == 0);
}

TEST_CASE("counters follow accepted submissions and buffer lifetimes") {
    TestSession session;
    REQUIRE(session.submit(StreamBuilder()
                               .create_buffer(buffer_handle(0), 8, MINEMOD_BUFFER_USAGE_VERTEX)
                               .create_buffer(buffer_handle(1), 24, MINEMOD_BUFFER_USAGE_INDEX)
                               .build())
            == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(StreamBuilder().destroy_buffer(buffer_handle(0)).build()) == MINEMOD_RESULT_OK);

    const auto counters = session.counters();
    CHECK(counters.submissions_accepted == 2);
    CHECK(counters.commands_executed == 3);
    CHECK(counters.buffers_created == 2);
    CHECK(counters.buffers_destroyed == 1);
    CHECK(counters.live_buffers == 1);
    CHECK(counters.live_buffer_bytes == 24);
}

TEST_CASE("session creation validates its configuration") {
    auto config = default_session_config();

    SUBCASE("unexpected struct size") {
        config.struct_bytes -= 8;
        CHECK(create_with(config) == MINEMOD_RESULT_INVALID_ARGUMENT);
    }
    SUBCASE("non-zero reserved field") {
        config.reserved = 1;
        CHECK(create_with(config) == MINEMOD_RESULT_INVALID_ARGUMENT);
    }
    SUBCASE("no buffer slots") {
        config.max_buffers = 0;
        CHECK(create_with(config) == MINEMOD_RESULT_INVALID_ARGUMENT);
    }
    SUBCASE("no buffer bytes") {
        config.max_buffer_bytes = 0;
        CHECK(create_with(config) == MINEMOD_RESULT_INVALID_ARGUMENT);
    }
    SUBCASE("another ABI generation") {
        config.abi_version.major += 1;
        CHECK(create_with(config) == MINEMOD_RESULT_INCOMPATIBLE_ABI);
    }
    SUBCASE("capability the library does not implement") {
        config.required_capabilities |= UINT64_C(1) << 40;
        CHECK(create_with(config) == MINEMOD_RESULT_UNSUPPORTED_CAPABILITY);
    }
    SUBCASE("more slots than the library allows") {
        config.max_buffers = MINEMOD_SESSION_MAX_BUFFERS + 1;
        CHECK(create_with(config) == MINEMOD_RESULT_LIMIT_EXCEEDED);
    }
    SUBCASE("more bytes than the library allows") {
        config.max_buffer_bytes = MINEMOD_SESSION_MAX_BUFFER_BYTES + 1;
        CHECK(create_with(config) == MINEMOD_RESULT_LIMIT_EXCEEDED);
    }
}

TEST_CASE("session entry points reject missing arguments") {
    TestSession session;
    const auto stream = StreamBuilder().build();
    auto config = default_session_config();
    minemod_session* created = nullptr;
    minemod_stream_report report{};
    minemod_session_counters counters{};
    minemod_buffer_description description{};
    std::uint8_t byte = 0;

    CHECK(minemod_session_create(nullptr, &created) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_session_create(&config, nullptr) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_session_submit(nullptr, stream.data(), stream.size(), &report) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_session_submit(session.get(), stream.data(), stream.size(), nullptr)
          == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_session_submit(session.get(), nullptr, stream.size(), &report) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_session_read_buffer(nullptr, buffer_handle(0), 0, &byte, 1) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_session_read_buffer(session.get(), buffer_handle(0), 0, nullptr, 1)
          == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_session_describe_buffer(session.get(), buffer_handle(0), nullptr)
          == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_session_get_counters(session.get(), nullptr) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_session_get_counters(nullptr, &counters) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_session_describe_buffer(nullptr, buffer_handle(0), &description)
          == MINEMOD_RESULT_INVALID_ARGUMENT);

    minemod_session_destroy(nullptr);
}
