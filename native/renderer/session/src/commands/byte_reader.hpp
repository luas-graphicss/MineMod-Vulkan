#pragma once

#include <array>
#include <bit>
#include <concepts>
#include <cstddef>
#include <cstdint>
#include <span>
#include <stdexcept>

namespace minemod::scene {

class ByteReader {
public:
    explicit ByteReader(std::span<const std::byte> bytes) noexcept : bytes_(bytes) {}

    template <std::unsigned_integral Value>
    Value read() {
        if (bytes_.size() - cursor_ < sizeof(Value)) {
            throw std::out_of_range("a scene command was read past its fixed fields");
        }
        Value value = 0;
        for (std::size_t index = 0; index < sizeof(Value); ++index) {
            const auto byte = static_cast<Value>(std::to_integer<Value>(bytes_[cursor_ + index]));
            value = static_cast<Value>(value | static_cast<Value>(byte << (index * 8)));
        }
        cursor_ += sizeof(Value);
        return value;
    }

    std::int32_t read_signed() {
        return static_cast<std::int32_t>(read<std::uint32_t>());
    }

    float read_float() {
        return std::bit_cast<float>(read<std::uint32_t>());
    }

    double read_double() {
        return std::bit_cast<double>(read<std::uint64_t>());
    }

    template <std::size_t Count>
    std::array<float, Count> read_floats() {
        std::array<float, Count> values{};
        for (float& value : values) {
            value = read_float();
        }
        return values;
    }

private:
    std::span<const std::byte> bytes_;
    std::size_t cursor_ = 0;
};

}
