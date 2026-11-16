#include "support/stream_builder.hpp"

#include "minemod/abi/command_stream.h"
#include "minemod/abi/version.h"

namespace minemod::abi::testing {
namespace {

template <typename Value>
void store(std::vector<std::uint8_t>& bytes, std::size_t offset, Value value) {
    for (std::size_t index = 0; index < sizeof(Value); ++index) {
        bytes[offset + index] = static_cast<std::uint8_t>(value >> (index * 8));
    }
}

template <typename Value>
void append(std::vector<std::uint8_t>& bytes, Value value) {
    const std::size_t offset = bytes.size();
    bytes.resize(offset + sizeof(Value));
    store(bytes, offset, value);
}

std::size_t aligned(std::size_t bytes) {
    return (bytes + MINEMOD_COMMAND_ALIGNMENT - 1) / MINEMOD_COMMAND_ALIGNMENT * MINEMOD_COMMAND_ALIGNMENT;
}

}

void store_u16(std::vector<std::uint8_t>& bytes, std::size_t offset, std::uint16_t value) {
    store(bytes, offset, value);
}

void store_u32(std::vector<std::uint8_t>& bytes, std::size_t offset, std::uint32_t value) {
    store(bytes, offset, value);
}

void store_u64(std::vector<std::uint8_t>& bytes, std::size_t offset, std::uint64_t value) {
    store(bytes, offset, value);
}

minemod_handle buffer_handle(std::uint32_t index, std::uint32_t generation) {
    return minemod_handle_pack(MINEMOD_HANDLE_KIND_BUFFER, generation, index);
}

StreamBuilder& StreamBuilder::create_buffer(minemod_handle handle, std::uint64_t size_bytes, std::uint32_t usage) {
    std::vector<std::uint8_t> payload;
    append(payload, handle);
    append(payload, size_bytes);
    append(payload, usage);
    append(payload, std::uint32_t{0});
    return command(MINEMOD_OPCODE_CREATE_BUFFER, payload);
}

StreamBuilder& StreamBuilder::write_buffer(
    minemod_handle handle, std::uint64_t offset, std::span<const std::uint8_t> data) {
    std::vector<std::uint8_t> payload;
    append(payload, handle);
    append(payload, offset);
    append(payload, static_cast<std::uint64_t>(data.size()));
    payload.insert(payload.end(), data.begin(), data.end());
    return command(MINEMOD_OPCODE_WRITE_BUFFER, payload);
}

StreamBuilder& StreamBuilder::destroy_buffer(minemod_handle handle) {
    std::vector<std::uint8_t> payload;
    append(payload, handle);
    return command(MINEMOD_OPCODE_DESTROY_BUFFER, payload);
}

StreamBuilder& StreamBuilder::command(std::uint16_t opcode, std::span<const std::uint8_t> payload) {
    const std::size_t record_bytes = MINEMOD_COMMAND_HEADER_BYTES + aligned(payload.size());
    append(commands_, opcode);
    append(commands_, std::uint16_t{0});
    append(commands_, static_cast<std::uint32_t>(record_bytes));
    commands_.insert(commands_.end(), payload.begin(), payload.end());
    commands_.resize(commands_.size() + aligned(payload.size()) - payload.size());
    ++command_count_;
    return *this;
}

std::vector<std::uint8_t> StreamBuilder::build() const {
    std::vector<std::uint8_t> stream;
    append(stream, std::uint32_t{MINEMOD_STREAM_MAGIC});
    append(stream, std::uint16_t{MINEMOD_ABI_VERSION_MAJOR});
    append(stream, std::uint16_t{MINEMOD_ABI_VERSION_MINOR});
    append(stream, command_count_);
    append(stream, static_cast<std::uint32_t>(MINEMOD_STREAM_HEADER_BYTES + commands_.size()));
    stream.insert(stream.end(), commands_.begin(), commands_.end());
    return stream;
}

}
