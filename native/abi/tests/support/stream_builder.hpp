#pragma once

#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

#include "minemod/abi/handle.h"

namespace minemod::abi::testing {

void store_u16(std::vector<std::uint8_t>& bytes, std::size_t offset, std::uint16_t value);
void store_u32(std::vector<std::uint8_t>& bytes, std::size_t offset, std::uint32_t value);
void store_u64(std::vector<std::uint8_t>& bytes, std::size_t offset, std::uint64_t value);

minemod_handle buffer_handle(std::uint32_t index, std::uint32_t generation = 1);

class StreamBuilder {
public:
    StreamBuilder& create_buffer(minemod_handle handle, std::uint64_t size_bytes, std::uint32_t usage);
    StreamBuilder& write_buffer(minemod_handle handle, std::uint64_t offset, std::span<const std::uint8_t> data);
    StreamBuilder& destroy_buffer(minemod_handle handle);
    StreamBuilder& command(std::uint16_t opcode, std::span<const std::uint8_t> payload);

    std::vector<std::uint8_t> build() const;

private:
    std::vector<std::uint8_t> commands_;
    std::uint32_t command_count_ = 0;
};

}
