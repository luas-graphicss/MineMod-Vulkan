#include "resources/buffer_registry.hpp"

#include <algorithm>
#include <utility>

namespace minemod::abi {

BufferRegistry::BufferRegistry(std::uint32_t max_buffers, std::uint64_t max_total_bytes) noexcept
    : max_buffers_(max_buffers), max_total_bytes_(max_total_bytes) {}

minemod_result BufferRegistry::create(
    minemod_handle handle, std::uint64_t size_bytes, std::uint32_t usage, std::uint64_t submission) {
    if (!names_slot(handle)) {
        return MINEMOD_RESULT_INVALID_HANDLE;
    }
    const std::size_t index = minemod_handle_index(handle);
    const std::uint32_t generation = minemod_handle_generation(handle);
    if (index < slots_.size()) {
        if (generation <= slots_[index].generation) {
            return MINEMOD_RESULT_STALE_HANDLE;
        }
        if (slots_[index].live) {
            return MINEMOD_RESULT_INVALID_HANDLE;
        }
    }
    if (size_bytes > max_total_bytes_ - counters_.live_bytes) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }

    std::vector<std::byte> bytes(static_cast<std::size_t>(size_bytes));
    if (index >= slots_.size()) {
        slots_.resize(index + 1);
    }
    slots_[index] = Slot{
        .generation = generation,
        .usage = usage,
        .last_used_submission = submission,
        .live = true,
        .bytes = std::move(bytes),
    };
    ++counters_.created;
    ++counters_.live;
    counters_.live_bytes += size_bytes;
    return MINEMOD_RESULT_OK;
}

minemod_result BufferRegistry::write(
    minemod_handle handle, std::uint64_t offset, std::span<const std::byte> data, std::uint64_t submission) {
    std::size_t index = 0;
    if (const auto located = locate(handle, index); located != MINEMOD_RESULT_OK) {
        return located;
    }
    Slot& slot = slots_[index];
    if (offset > slot.bytes.size() || data.size() > slot.bytes.size() - offset) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    std::copy(data.begin(), data.end(), slot.bytes.begin() + static_cast<std::ptrdiff_t>(offset));
    slot.last_used_submission = submission;
    return MINEMOD_RESULT_OK;
}

minemod_result BufferRegistry::destroy(minemod_handle handle) {
    std::size_t index = 0;
    if (const auto located = locate(handle, index); located != MINEMOD_RESULT_OK) {
        return located;
    }
    Slot& slot = slots_[index];
    counters_.live_bytes -= slot.bytes.size();
    --counters_.live;
    ++counters_.destroyed;
    slot.bytes = std::vector<std::byte>{};
    slot.live = false;
    return MINEMOD_RESULT_OK;
}

minemod_result BufferRegistry::read(
    minemod_handle handle, std::uint64_t offset, std::span<std::byte> destination) const {
    std::size_t index = 0;
    if (const auto located = locate(handle, index); located != MINEMOD_RESULT_OK) {
        return located;
    }
    const Slot& slot = slots_[index];
    if (offset > slot.bytes.size() || destination.size() > slot.bytes.size() - offset) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    const auto first = slot.bytes.begin() + static_cast<std::ptrdiff_t>(offset);
    std::copy(first, first + static_cast<std::ptrdiff_t>(destination.size()), destination.begin());
    return MINEMOD_RESULT_OK;
}

minemod_result BufferRegistry::describe(minemod_handle handle, minemod_buffer_description& description) const {
    std::size_t index = 0;
    if (const auto located = locate(handle, index); located != MINEMOD_RESULT_OK) {
        return located;
    }
    const Slot& slot = slots_[index];
    description = minemod_buffer_description{
        .size_bytes = slot.bytes.size(),
        .last_used_submission = slot.last_used_submission,
        .usage = slot.usage,
        .generation = slot.generation,
    };
    return MINEMOD_RESULT_OK;
}

const BufferCounters& BufferRegistry::counters() const noexcept {
    return counters_;
}

bool BufferRegistry::names_slot(minemod_handle handle) const noexcept {
    return minemod_handle_kind(handle) == MINEMOD_HANDLE_KIND_BUFFER
        && minemod_handle_generation(handle) != 0
        && minemod_handle_index(handle) < max_buffers_;
}

minemod_result BufferRegistry::locate(minemod_handle handle, std::size_t& index) const noexcept {
    if (!names_slot(handle) || minemod_handle_index(handle) >= slots_.size()) {
        return MINEMOD_RESULT_INVALID_HANDLE;
    }
    index = minemod_handle_index(handle);
    const Slot& slot = slots_[index];
    const std::uint32_t generation = minemod_handle_generation(handle);
    if (generation > slot.generation) {
        return MINEMOD_RESULT_INVALID_HANDLE;
    }
    if (generation < slot.generation || !slot.live) {
        return MINEMOD_RESULT_STALE_HANDLE;
    }
    return MINEMOD_RESULT_OK;
}

}
