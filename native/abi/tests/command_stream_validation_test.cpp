#include <doctest.h>

#include <cstdint>
#include <vector>

#include "minemod/abi/command_stream.h"
#include "minemod/abi/version.h"
#include "support/stream_builder.hpp"

using namespace minemod::abi::testing;

namespace {

constexpr std::size_t magic_offset = 0;
constexpr std::size_t major_offset = 4;
constexpr std::size_t minor_offset = 6;
constexpr std::size_t command_count_offset = 8;
constexpr std::size_t total_bytes_offset = 12;

constexpr std::size_t create_record = MINEMOD_STREAM_HEADER_BYTES;
constexpr std::size_t create_flags = create_record + 2;
constexpr std::size_t create_record_bytes = create_record + 4;
constexpr std::size_t create_size = create_record + 16;
constexpr std::size_t create_usage = create_record + 24;
constexpr std::size_t create_reserved = create_record + 28;

constexpr std::size_t write_record = create_record + 32;
constexpr std::size_t write_offset = write_record + 16;
constexpr std::size_t write_data_bytes = write_record + 24;

constexpr std::size_t destroy_record = write_record + 40;
constexpr std::size_t lifecycle_bytes = destroy_record + 16;

struct Validation {
    minemod_result result;
    minemod_stream_report report;
};

Validation validate(const std::vector<std::uint8_t>& stream) {
    Validation validation{};
    validation.result = minemod_validate_command_stream(stream.data(), stream.size(), &validation.report);
    return validation;
}

std::vector<std::uint8_t> lifecycle_stream() {
    const std::uint8_t data[] = {1, 2, 3, 4, 5};
    return StreamBuilder()
        .create_buffer(buffer_handle(0), 16, MINEMOD_BUFFER_USAGE_VERTEX)
        .write_buffer(buffer_handle(0), 4, data)
        .destroy_buffer(buffer_handle(0))
        .build();
}

}

TEST_CASE("a stream without commands is valid") {
    const auto validation = validate(StreamBuilder().build());

    CHECK(validation.result == MINEMOD_RESULT_OK);
    CHECK(validation.report.command_count == 0);
    CHECK(validation.report.failed_command == MINEMOD_NO_COMMAND);
    CHECK(validation.report.failed_offset == MINEMOD_NO_OFFSET);
}

TEST_CASE("a buffer lifecycle stream is valid and reports its commands") {
    const auto stream = lifecycle_stream();
    REQUIRE(stream.size() == lifecycle_bytes);

    const auto validation = validate(stream);

    CHECK(validation.result == MINEMOD_RESULT_OK);
    CHECK(validation.report.command_count == 3);
}

TEST_CASE("truncated streams are rejected") {
    auto stream = lifecycle_stream();

    SUBCASE("shorter than the stream header") {
        stream.resize(MINEMOD_STREAM_HEADER_BYTES - 1);
        const auto validation = validate(stream);
        CHECK(validation.result == MINEMOD_RESULT_TRUNCATED_STREAM);
        CHECK(validation.report.failed_command == MINEMOD_NO_COMMAND);
        CHECK(validation.report.failed_offset == 0);
    }
    SUBCASE("shorter than the declared total") {
        stream.resize(stream.size() - 8);
        CHECK(validate(stream).result == MINEMOD_RESULT_TRUNCATED_STREAM);
    }
    SUBCASE("fewer records than the declared command count") {
        store_u32(stream, command_count_offset, 4);
        const auto validation = validate(stream);
        CHECK(validation.result == MINEMOD_RESULT_TRUNCATED_STREAM);
        CHECK(validation.report.command_count == 3);
        CHECK(validation.report.failed_command == 3);
        CHECK(validation.report.failed_offset == lifecycle_bytes);
    }
}

TEST_CASE("streams that break the framing are malformed") {
    auto stream = lifecycle_stream();

    SUBCASE("wrong magic") {
        store_u32(stream, magic_offset, 0x12345678);
    }
    SUBCASE("non-zero command flags") {
        store_u16(stream, create_flags, 1);
    }
    SUBCASE("more records than the declared command count") {
        store_u32(stream, command_count_offset, 2);
    }
    SUBCASE("bytes after the declared total") {
        stream.resize(stream.size() + 8);
    }
    SUBCASE("zero-sized buffer") {
        store_u64(stream, create_size, 0);
    }
    SUBCASE("unknown buffer usage") {
        store_u32(stream, create_usage, 0x80);
    }
    SUBCASE("no buffer usage") {
        store_u32(stream, create_usage, 0);
    }
    SUBCASE("non-zero reserved field") {
        store_u32(stream, create_reserved, 1);
    }
    SUBCASE("empty write") {
        auto empty_write = StreamBuilder().write_buffer(buffer_handle(0), 0, {}).build();
        stream = empty_write;
    }

    CHECK(validate(stream).result == MINEMOD_RESULT_MALFORMED_STREAM);
}

TEST_CASE("streams from another ABI generation are rejected") {
    auto stream = lifecycle_stream();

    SUBCASE("different major") {
        store_u16(stream, major_offset, MINEMOD_ABI_VERSION_MAJOR + 1);
    }
    SUBCASE("different minor while the ABI is unstable") {
        store_u16(stream, minor_offset, MINEMOD_ABI_VERSION_MINOR + 1);
    }

    CHECK(validate(stream).result == MINEMOD_RESULT_INCOMPATIBLE_ABI);
}

TEST_CASE("an unknown opcode is rejected at its record") {
    const auto stream = StreamBuilder()
                            .destroy_buffer(buffer_handle(0))
                            .command(0x7FFF, {})
                            .build();

    const auto validation = validate(stream);

    CHECK(validation.result == MINEMOD_RESULT_UNKNOWN_OPCODE);
    CHECK(validation.report.command_count == 1);
    CHECK(validation.report.failed_command == 1);
    CHECK(validation.report.failed_offset == MINEMOD_STREAM_HEADER_BYTES + 16);
}

TEST_CASE("record lengths outside the stream or the command layout are rejected") {
    auto stream = lifecycle_stream();

    SUBCASE("record longer than the remaining stream") {
        store_u32(stream, create_record_bytes, static_cast<std::uint32_t>(stream.size()));
    }
    SUBCASE("record shorter than its own header") {
        store_u32(stream, create_record_bytes, 4);
    }
    SUBCASE("fixed-size command with extra bytes") {
        const std::uint8_t oversized_payload[32] = {};
        stream = StreamBuilder().command(MINEMOD_OPCODE_CREATE_BUFFER, oversized_payload).build();
    }
    SUBCASE("write data longer than its record") {
        store_u64(stream, write_data_bytes, 64);
    }
    SUBCASE("write record with a full alignment unit of slack") {
        store_u64(stream, write_data_bytes, 0);
    }
    SUBCASE("write record shorter than its fixed fields") {
        const std::uint8_t short_payload[8] = {};
        stream = StreamBuilder().command(MINEMOD_OPCODE_WRITE_BUFFER, short_payload).build();
    }

    CHECK(validate(stream).result == MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS);
}

TEST_CASE("misaligned lengths are rejected") {
    auto stream = lifecycle_stream();

    SUBCASE("record length") {
        store_u32(stream, create_record_bytes, 28);
    }
    SUBCASE("stream length") {
        stream.resize(MINEMOD_STREAM_HEADER_BYTES + 4);
        store_u32(stream, command_count_offset, 0);
        store_u32(stream, total_bytes_offset, static_cast<std::uint32_t>(stream.size()));
    }

    CHECK(validate(stream).result == MINEMOD_RESULT_MISALIGNED);
}

TEST_CASE("a write range that wraps the address space is rejected") {
    auto stream = lifecycle_stream();
    store_u64(stream, write_offset, UINT64_MAX - 2);

    const auto validation = validate(stream);

    CHECK(validation.result == MINEMOD_RESULT_ARITHMETIC_OVERFLOW);
    CHECK(validation.report.failed_command == 1);
    CHECK(validation.report.failed_offset == write_record);
}

TEST_CASE("streams above the negotiated limits are rejected") {
    SUBCASE("too many commands") {
        auto stream = StreamBuilder().build();
        store_u32(stream, command_count_offset, MINEMOD_STREAM_MAX_COMMANDS + 1);
        CHECK(validate(stream).result == MINEMOD_RESULT_LIMIT_EXCEEDED);
    }
    SUBCASE("too many bytes") {
        const std::vector<std::uint8_t> stream(MINEMOD_STREAM_MAX_BYTES + 8);
        CHECK(validate(stream).result == MINEMOD_RESULT_LIMIT_EXCEEDED);
    }
}

TEST_CASE("validation rejects missing arguments") {
    const auto stream = lifecycle_stream();
    minemod_stream_report report{};

    CHECK(minemod_validate_command_stream(stream.data(), stream.size(), nullptr) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_validate_command_stream(nullptr, stream.size(), &report) == MINEMOD_RESULT_INVALID_ARGUMENT);
}
