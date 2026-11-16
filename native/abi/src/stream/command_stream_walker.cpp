#include "stream/command_stream_walker.hpp"

#include <limits>

#include "minemod/abi/version.h"
#include "negotiation/abi_compatibility.hpp"
#include "stream/little_endian.hpp"

namespace minemod::abi {
namespace {

constexpr std::size_t stream_magic_offset = 0;
constexpr std::size_t stream_major_offset = 4;
constexpr std::size_t stream_minor_offset = 6;
constexpr std::size_t stream_command_count_offset = 8;
constexpr std::size_t stream_total_bytes_offset = 12;

constexpr std::size_t command_opcode_offset = 0;
constexpr std::size_t command_flags_offset = 2;
constexpr std::size_t command_bytes_offset = 4;

constexpr std::size_t create_buffer_bytes = MINEMOD_COMMAND_HEADER_BYTES + 24;
constexpr std::size_t write_buffer_fixed_bytes = MINEMOD_COMMAND_HEADER_BYTES + 24;
constexpr std::size_t destroy_buffer_bytes = MINEMOD_COMMAND_HEADER_BYTES + 8;

constexpr std::uint32_t known_buffer_usage =
    MINEMOD_BUFFER_USAGE_VERTEX | MINEMOD_BUFFER_USAGE_INDEX | MINEMOD_BUFFER_USAGE_UNIFORM;

class DiscardingSink final : public CommandSink {
public:
    minemod_result create_buffer(const CreateBufferCommand&) override {
        return MINEMOD_RESULT_OK;
    }

    minemod_result write_buffer(const WriteBufferCommand&) override {
        return MINEMOD_RESULT_OK;
    }

    minemod_result destroy_buffer(const DestroyBufferCommand&) override {
        return MINEMOD_RESULT_OK;
    }
};

struct Record {
    std::uint16_t opcode = 0;
    std::span<const std::byte> bytes;
};

minemod_result read_stream_header(std::span<const std::byte> stream, std::uint32_t& command_count) noexcept {
    if (stream.size() > MINEMOD_STREAM_MAX_BYTES) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    if (stream.size() < MINEMOD_STREAM_HEADER_BYTES) {
        return MINEMOD_RESULT_TRUNCATED_STREAM;
    }
    if (read_little_endian<std::uint32_t>(stream, stream_magic_offset) != MINEMOD_STREAM_MAGIC) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    const minemod_abi_version version{
        read_little_endian<std::uint16_t>(stream, stream_major_offset),
        read_little_endian<std::uint16_t>(stream, stream_minor_offset),
    };
    if (!accepts_client(version)) {
        return MINEMOD_RESULT_INCOMPATIBLE_ABI;
    }
    const auto total_bytes = read_little_endian<std::uint32_t>(stream, stream_total_bytes_offset);
    if (total_bytes > stream.size()) {
        return MINEMOD_RESULT_TRUNCATED_STREAM;
    }
    if (total_bytes < stream.size()) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    if (total_bytes % MINEMOD_COMMAND_ALIGNMENT != 0) {
        return MINEMOD_RESULT_MISALIGNED;
    }
    command_count = read_little_endian<std::uint32_t>(stream, stream_command_count_offset);
    if (command_count > MINEMOD_STREAM_MAX_COMMANDS) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    return MINEMOD_RESULT_OK;
}

minemod_result frame_record(std::span<const std::byte> remaining, Record& record) noexcept {
    if (remaining.size() < MINEMOD_COMMAND_HEADER_BYTES) {
        return MINEMOD_RESULT_TRUNCATED_STREAM;
    }
    const auto record_bytes = read_little_endian<std::uint32_t>(remaining, command_bytes_offset);
    if (record_bytes < MINEMOD_COMMAND_HEADER_BYTES || record_bytes > remaining.size()) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    if (record_bytes % MINEMOD_COMMAND_ALIGNMENT != 0) {
        return MINEMOD_RESULT_MISALIGNED;
    }
    if (read_little_endian<std::uint16_t>(remaining, command_flags_offset) != 0) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    record.opcode = read_little_endian<std::uint16_t>(remaining, command_opcode_offset);
    record.bytes = remaining.first(record_bytes);
    return MINEMOD_RESULT_OK;
}

minemod_result dispatch_create_buffer(std::span<const std::byte> record, CommandSink& sink) {
    if (record.size() != create_buffer_bytes) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    const auto payload = record.subspan(MINEMOD_COMMAND_HEADER_BYTES);
    const CreateBufferCommand command{
        .handle = read_little_endian<std::uint64_t>(payload, 0),
        .size_bytes = read_little_endian<std::uint64_t>(payload, 8),
        .usage = read_little_endian<std::uint32_t>(payload, 16),
    };
    const auto reserved = read_little_endian<std::uint32_t>(payload, 20);
    const bool has_known_usage = command.usage != 0 && (command.usage & ~known_buffer_usage) == 0;
    if (reserved != 0 || command.size_bytes == 0 || !has_known_usage) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    return sink.create_buffer(command);
}

minemod_result dispatch_write_buffer(std::span<const std::byte> record, CommandSink& sink) {
    if (record.size() < write_buffer_fixed_bytes) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    const auto payload = record.subspan(MINEMOD_COMMAND_HEADER_BYTES);
    const auto handle = read_little_endian<std::uint64_t>(payload, 0);
    const auto offset = read_little_endian<std::uint64_t>(payload, 8);
    const auto data_bytes = read_little_endian<std::uint64_t>(payload, 16);

    const std::uint64_t data_capacity = record.size() - write_buffer_fixed_bytes;
    if (data_bytes > data_capacity || data_capacity - data_bytes >= MINEMOD_COMMAND_ALIGNMENT) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    if (data_bytes == 0) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    if (offset > std::numeric_limits<std::uint64_t>::max() - data_bytes) {
        return MINEMOD_RESULT_ARITHMETIC_OVERFLOW;
    }
    const auto data = record.subspan(write_buffer_fixed_bytes, static_cast<std::size_t>(data_bytes));
    return sink.write_buffer(WriteBufferCommand{.handle = handle, .offset = offset, .data = data});
}

minemod_result dispatch_destroy_buffer(std::span<const std::byte> record, CommandSink& sink) {
    if (record.size() != destroy_buffer_bytes) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    const auto payload = record.subspan(MINEMOD_COMMAND_HEADER_BYTES);
    return sink.destroy_buffer(DestroyBufferCommand{.handle = read_little_endian<std::uint64_t>(payload, 0)});
}

minemod_result dispatch(const Record& record, CommandSink& sink) {
    switch (record.opcode) {
        case MINEMOD_OPCODE_CREATE_BUFFER:
            return dispatch_create_buffer(record.bytes, sink);
        case MINEMOD_OPCODE_WRITE_BUFFER:
            return dispatch_write_buffer(record.bytes, sink);
        case MINEMOD_OPCODE_DESTROY_BUFFER:
            return dispatch_destroy_buffer(record.bytes, sink);
        default:
            return MINEMOD_RESULT_UNKNOWN_OPCODE;
    }
}

minemod_result fail(
    minemod_stream_report& report, minemod_result result, std::uint32_t command, std::uint64_t offset) noexcept {
    report.failed_command = command;
    report.failed_offset = offset;
    return result;
}

}

minemod_result walk_command_stream(
    std::span<const std::byte> stream, CommandSink& sink, minemod_stream_report& report) {
    report = minemod_stream_report{
        .command_count = 0,
        .failed_command = MINEMOD_NO_COMMAND,
        .failed_offset = MINEMOD_NO_OFFSET,
    };

    std::uint32_t command_count = 0;
    if (const auto header = read_stream_header(stream, command_count); header != MINEMOD_RESULT_OK) {
        return fail(report, header, MINEMOD_NO_COMMAND, 0);
    }

    std::size_t cursor = MINEMOD_STREAM_HEADER_BYTES;
    for (std::uint32_t index = 0; index < command_count; ++index) {
        Record record;
        auto result = frame_record(stream.subspan(cursor), record);
        if (result == MINEMOD_RESULT_OK) {
            result = dispatch(record, sink);
        }
        if (result != MINEMOD_RESULT_OK) {
            return fail(report, result, index, cursor);
        }
        cursor += record.bytes.size();
        report.command_count = index + 1;
    }

    if (cursor != stream.size()) {
        return fail(report, MINEMOD_RESULT_MALFORMED_STREAM, MINEMOD_NO_COMMAND, cursor);
    }
    return MINEMOD_RESULT_OK;
}

minemod_result validate_command_stream(std::span<const std::byte> stream, minemod_stream_report& report) {
    DiscardingSink sink;
    return walk_command_stream(stream, sink, report);
}

}
