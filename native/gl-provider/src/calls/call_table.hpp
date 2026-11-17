#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <span>
#include <string_view>

namespace minemod::provider {

inline constexpr std::size_t max_call_words = 16;

struct CallRecord {
    std::string_view name;
    std::uint8_t word_count;
    void* thunk;
    std::int64_t (*invoke)(void* address, std::span<const std::int64_t> words);
};

template <typename Function>
void* thunk_address(Function* thunk) noexcept {
    return reinterpret_cast<void*>(thunk);
}

std::span<const CallRecord> call_records() noexcept;

std::optional<std::uint32_t> find_call(std::string_view name) noexcept;

std::optional<std::int64_t> invoke_call(std::uint32_t function, void* address, std::span<const std::int64_t> words);

}
