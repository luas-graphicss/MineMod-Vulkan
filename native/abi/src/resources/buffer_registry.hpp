#pragma once

#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

#include "minemod/abi/handle.h"
#include "minemod/abi/result.h"
#include "minemod/abi/session.h"

namespace minemod::abi {

struct BufferCounters {
    std::uint64_t created = 0;
    std::uint64_t destroyed = 0;
    std::uint64_t live = 0;
    std::uint64_t live_bytes = 0;
};

class BufferRegistry {
public:
    BufferRegistry(std::uint32_t max_buffers, std::uint64_t max_total_bytes) noexcept;

    minemod_result create(
        minemod_handle handle, std::uint64_t size_bytes, std::uint32_t usage, std::uint64_t submission);
    minemod_result write(
        minemod_handle handle, std::uint64_t offset, std::span<const std::byte> data, std::uint64_t submission);
    minemod_result destroy(minemod_handle handle);

    minemod_result read(minemod_handle handle, std::uint64_t offset, std::span<std::byte> destination) const;
    minemod_result describe(minemod_handle handle, minemod_buffer_description& description) const;
    const BufferCounters& counters() const noexcept;

private:
    struct Slot {
        std::uint32_t generation = 0;
        std::uint32_t usage = 0;
        std::uint64_t last_used_submission = 0;
        bool live = false;
        std::vector<std::byte> bytes;
    };

    bool names_slot(minemod_handle handle) const noexcept;
    minemod_result locate(minemod_handle handle, std::size_t& index) const noexcept;

    std::uint32_t max_buffers_;
    std::uint64_t max_total_bytes_;
    std::vector<Slot> slots_;
    BufferCounters counters_;
};

}
