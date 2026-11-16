#pragma once

#include <cstddef>
#include <cstdint>
#include <span>
#include <stdexcept>
#include <vector>

namespace minemod::renderer {

class ShaderError : public std::runtime_error {
public:
    using std::runtime_error::runtime_error;
};

struct SpirvLimits {
    std::size_t max_words;
    std::uint32_t max_id_bound;
};

inline constexpr SpirvLimits default_spirv_limits{std::size_t{1} << 20, std::uint32_t{1} << 16};

struct SpirvInstruction {
    std::uint16_t opcode;
    std::span<const std::uint32_t> operands;
};

class SpirvModule {
public:
    explicit SpirvModule(std::span<const std::uint32_t> words, const SpirvLimits& limits = default_spirv_limits);

    SpirvModule(SpirvModule&&) noexcept = default;
    SpirvModule& operator=(SpirvModule&&) noexcept = default;
    SpirvModule(const SpirvModule&) = delete;
    SpirvModule& operator=(const SpirvModule&) = delete;

    std::span<const std::uint32_t> words() const noexcept {
        return words_;
    }

    std::span<const SpirvInstruction> instructions() const noexcept {
        return instructions_;
    }

    std::uint32_t id_bound() const noexcept;

private:
    std::vector<std::uint32_t> words_;
    std::vector<SpirvInstruction> instructions_;
};

}
