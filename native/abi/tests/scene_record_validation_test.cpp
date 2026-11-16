#include <doctest.h>

#include <array>
#include <cstddef>
#include <cstdint>
#include <vector>

#include "minemod/abi/command_stream.h"
#include "minemod/abi/scene_commands.h"
#include "support/stream_builder.hpp"
#include "support/test_session.hpp"

using namespace minemod::abi::testing;

namespace {

struct FixedCommand {
    std::uint16_t opcode;
    std::size_t payload_bytes;
};

constexpr std::array fixed_commands{
    FixedCommand{MINEMOD_OPCODE_DEFINE_TEXTURE, MINEMOD_DEFINE_TEXTURE_BYTES},
    FixedCommand{MINEMOD_OPCODE_DESTROY_TEXTURE, MINEMOD_DESTROY_TEXTURE_BYTES},
    FixedCommand{MINEMOD_OPCODE_UNLOAD_SECTION, MINEMOD_UNLOAD_SECTION_BYTES},
    FixedCommand{MINEMOD_OPCODE_CHANGE_WORLD, MINEMOD_CHANGE_WORLD_BYTES},
    FixedCommand{MINEMOD_OPCODE_BEGIN_FRAME, MINEMOD_BEGIN_FRAME_BYTES},
    FixedCommand{MINEMOD_OPCODE_SET_VIEW, MINEMOD_SET_VIEW_BYTES},
    FixedCommand{MINEMOD_OPCODE_SET_TERRAIN, MINEMOD_SET_TERRAIN_BYTES},
    FixedCommand{MINEMOD_OPCODE_END_FRAME, MINEMOD_END_FRAME_BYTES},
    FixedCommand{MINEMOD_OPCODE_SET_CLIP, MINEMOD_SET_CLIP_BYTES},
    FixedCommand{MINEMOD_OPCODE_CLEAR_CLIP, MINEMOD_CLEAR_CLIP_BYTES},
    FixedCommand{MINEMOD_OPCODE_SET_TEXTURE_TRANSFORM, MINEMOD_SET_TEXTURE_TRANSFORM_BYTES},
    FixedCommand{MINEMOD_OPCODE_SET_QUAD_ORDER, MINEMOD_SET_QUAD_ORDER_BYTES},
    FixedCommand{MINEMOD_OPCODE_CONNECT_SECTION_FACES, MINEMOD_CONNECT_SECTION_FACES_BYTES},
    FixedCommand{MINEMOD_OPCODE_CLEAR_DEPTH, MINEMOD_CLEAR_DEPTH_BYTES},
    FixedCommand{MINEMOD_OPCODE_BLUR_FRAME, MINEMOD_BLUR_FRAME_BYTES},
};

constexpr std::size_t texel_length_field = MINEMOD_WRITE_TEXTURE_BYTES - sizeof(std::uint64_t);
constexpr std::size_t second_record = MINEMOD_STREAM_HEADER_BYTES + MINEMOD_COMMAND_HEADER_BYTES
    + MINEMOD_DEFINE_TEXTURE_BYTES;

std::vector<std::uint8_t> texture_write(std::uint64_t declared_texel_bytes, std::size_t carried_bytes) {
    std::vector<std::uint8_t> payload(MINEMOD_WRITE_TEXTURE_BYTES + carried_bytes, 0);
    store_u64(payload, texel_length_field, declared_texel_bytes);
    return payload;
}

std::vector<std::uint8_t> texture_stream() {
    const std::vector<std::uint8_t> definition(MINEMOD_DEFINE_TEXTURE_BYTES, 0);
    return StreamBuilder()
        .command(MINEMOD_OPCODE_DEFINE_TEXTURE, definition)
        .command(MINEMOD_OPCODE_WRITE_TEXTURE, texture_write(4, 4))
        .command(MINEMOD_OPCODE_END_FRAME, {})
        .build();
}

minemod_result validate(const std::vector<std::uint8_t>& stream) {
    minemod_stream_report report{};
    return minemod_validate_command_stream(stream.data(), stream.size(), &report);
}

struct VisitedRecord {
    std::uint16_t opcode;
    std::uint32_t payload_bytes;

    friend bool operator==(const VisitedRecord&, const VisitedRecord&) = default;
};

struct Visit {
    std::vector<VisitedRecord> records;
    std::size_t refused_record = SIZE_MAX;
};

minemod_result remember_record(void* context, const minemod_command_record* record) {
    auto& visit = *static_cast<Visit*>(context);
    if (visit.records.size() == visit.refused_record) {
        return MINEMOD_RESULT_INVALID_HANDLE;
    }
    visit.records.push_back({record->opcode, record->payload_bytes});
    return MINEMOD_RESULT_OK;
}

}

TEST_CASE("a scene command without a blob is valid only at its exact size") {
    for (const FixedCommand& command : fixed_commands) {
        CAPTURE(command.opcode);
        const std::vector<std::uint8_t> exact(command.payload_bytes, 0);
        const std::vector<std::uint8_t> oversized(command.payload_bytes + MINEMOD_COMMAND_ALIGNMENT, 0);

        CHECK(validate(StreamBuilder().command(command.opcode, exact).build()) == MINEMOD_RESULT_OK);
        CHECK(
            validate(StreamBuilder().command(command.opcode, oversized).build())
            == MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS);
    }
}

TEST_CASE("a blob is valid when its declared length fills the record up to the padding") {
    CHECK(
        validate(StreamBuilder().command(MINEMOD_OPCODE_WRITE_TEXTURE, texture_write(5, 5)).build())
        == MINEMOD_RESULT_OK);
}

TEST_CASE("a blob that disagrees with its record is rejected") {
    std::vector<std::uint8_t> payload;

    SUBCASE("declared longer than the record") {
        payload = texture_write(64, 8);
    }
    SUBCASE("a full alignment unit left unused") {
        payload = texture_write(8, 16);
    }
    SUBCASE("record shorter than the fixed fields") {
        payload.assign(MINEMOD_WRITE_TEXTURE_BYTES - MINEMOD_COMMAND_ALIGNMENT, 0);
    }

    CHECK(
        validate(StreamBuilder().command(MINEMOD_OPCODE_WRITE_TEXTURE, payload).build())
        == MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS);
}

TEST_CASE("an empty blob is malformed") {
    CHECK(
        validate(StreamBuilder().command(MINEMOD_OPCODE_WRITE_TEXTURE, texture_write(0, 0)).build())
        == MINEMOD_RESULT_MALFORMED_STREAM);
}

TEST_CASE("a visitor receives every record of a valid stream in order") {
    const auto stream = texture_stream();
    Visit visit;
    minemod_stream_report report{};

    const minemod_result result =
        minemod_visit_command_stream(stream.data(), stream.size(), remember_record, &visit, &report);

    CHECK(result == MINEMOD_RESULT_OK);
    CHECK(report.command_count == 3);
    CHECK(
        visit.records
        == std::vector<VisitedRecord>{
            {MINEMOD_OPCODE_DEFINE_TEXTURE, MINEMOD_DEFINE_TEXTURE_BYTES},
            {MINEMOD_OPCODE_WRITE_TEXTURE, MINEMOD_WRITE_TEXTURE_BYTES + MINEMOD_COMMAND_ALIGNMENT},
            {MINEMOD_OPCODE_END_FRAME, 0},
        });
}

TEST_CASE("a visitor is not called for a stream that fails validation") {
    const auto stream = StreamBuilder()
                            .command(MINEMOD_OPCODE_END_FRAME, {})
                            .command(MINEMOD_OPCODE_WRITE_TEXTURE, texture_write(0, 0))
                            .build();
    Visit visit;
    minemod_stream_report report{};

    const minemod_result result =
        minemod_visit_command_stream(stream.data(), stream.size(), remember_record, &visit, &report);

    CHECK(result == MINEMOD_RESULT_MALFORMED_STREAM);
    CHECK(report.failed_command == 1);
    CHECK(visit.records.empty());
}

TEST_CASE("the result of a visitor stops the walk and names its record") {
    const auto stream = texture_stream();
    Visit visit{.records = {}, .refused_record = 1};
    minemod_stream_report report{};

    const minemod_result result =
        minemod_visit_command_stream(stream.data(), stream.size(), remember_record, &visit, &report);

    CHECK(result == MINEMOD_RESULT_INVALID_HANDLE);
    CHECK(report.command_count == 1);
    CHECK(report.failed_command == 1);
    CHECK(report.failed_offset == second_record);
    CHECK(visit.records.size() == 1);
}

TEST_CASE("visiting rejects missing arguments") {
    const auto stream = texture_stream();
    Visit visit;
    minemod_stream_report report{};

    CHECK(
        minemod_visit_command_stream(stream.data(), stream.size(), nullptr, &visit, &report)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(
        minemod_visit_command_stream(stream.data(), stream.size(), remember_record, &visit, nullptr)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(
        minemod_visit_command_stream(nullptr, stream.size(), remember_record, &visit, &report)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
}

TEST_CASE("a session without a renderer refuses scene commands") {
    TestSession session;

    CHECK(session.submit(texture_stream()) == MINEMOD_RESULT_UNSUPPORTED_CAPABILITY);
    CHECK(session.counters().submissions_aborted == 1);
}
