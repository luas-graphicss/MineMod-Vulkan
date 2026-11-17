#pragma once

#include <bit>
#include <cstdint>
#include <type_traits>

namespace minemod::provider {

template <typename Argument>
std::int64_t pack(Argument argument) noexcept {
    if constexpr (std::is_pointer_v<Argument>) {
        return static_cast<std::int64_t>(reinterpret_cast<std::uintptr_t>(argument));
    } else if constexpr (std::is_same_v<Argument, float>) {
        return static_cast<std::int64_t>(std::bit_cast<std::uint32_t>(argument));
    } else if constexpr (std::is_same_v<Argument, double>) {
        return std::bit_cast<std::int64_t>(argument);
    } else {
        return static_cast<std::int64_t>(argument);
    }
}

template <typename Argument>
Argument unpack(std::int64_t word) noexcept {
    if constexpr (std::is_pointer_v<Argument>) {
        return reinterpret_cast<Argument>(static_cast<std::uintptr_t>(word));
    } else if constexpr (std::is_same_v<Argument, float>) {
        return std::bit_cast<float>(static_cast<std::uint32_t>(word));
    } else if constexpr (std::is_same_v<Argument, double>) {
        return std::bit_cast<double>(word);
    } else {
        return static_cast<Argument>(word);
    }
}

}
