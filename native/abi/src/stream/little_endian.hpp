#pragma once

#include <concepts>
#include <cstddef>
#include <span>

namespace minemod::abi {

template <std::unsigned_integral Value>
Value read_little_endian(std::span<const std::byte> bytes, std::size_t offset) noexcept {
    Value value = 0;
    for (std::size_t index = 0; index < sizeof(Value); ++index) {
        const auto byte = static_cast<Value>(std::to_integer<Value>(bytes[offset + index]));
        value = static_cast<Value>(value | static_cast<Value>(byte << (index * 8)));
    }
    return value;
}

}
