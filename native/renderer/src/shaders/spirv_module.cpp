#include "shaders/spirv_module.hpp"

namespace minemod::renderer {

namespace {

constexpr std::uint32_t spirv_magic = 0x07230203;
constexpr std::uint32_t oldest_version = 0x00010000;
constexpr std::uint32_t newest_version = 0x00010600;
constexpr std::size_t magic_word = 0;
constexpr std::size_t version_word = 1;
constexpr std::size_t id_bound_word = 3;
constexpr std::size_t schema_word = 4;
constexpr std::size_t header_words = 5;

void require_header(std::span<const std::uint32_t> words, const SpirvLimits& limits) {
    if (words.size() < header_words) {
        throw ShaderError("the SPIR-V module is shorter than its header");
    }
    if (words.size() > limits.max_words) {
        throw ShaderError("the SPIR-V module is larger than the accepted size");
    }
    if (words[magic_word] != spirv_magic) {
        throw ShaderError("the module does not start with the SPIR-V magic number");
    }
    if (words[version_word] < oldest_version || words[version_word] > newest_version) {
        throw ShaderError("the SPIR-V version is outside 1.0 to 1.6");
    }
    if (words[id_bound_word] == 0 || words[id_bound_word] > limits.max_id_bound) {
        throw ShaderError("the SPIR-V id bound is outside the accepted range");
    }
    if (words[schema_word] != 0) {
        throw ShaderError("the SPIR-V schema is not zero");
    }
}

}

SpirvModule::SpirvModule(std::span<const std::uint32_t> words, const SpirvLimits& limits) {
    require_header(words, limits);
    words_.assign(words.begin(), words.end());

    const std::span<const std::uint32_t> stored(words_);
    std::size_t next = header_words;
    while (next < stored.size()) {
        const std::size_t word_count = stored[next] >> 16;
        if (word_count == 0 || word_count > stored.size() - next) {
            throw ShaderError("a SPIR-V instruction does not fit inside the module");
        }
        const auto opcode = static_cast<std::uint16_t>(stored[next] & 0xffffu);
        instructions_.push_back(SpirvInstruction{opcode, stored.subspan(next + 1, word_count - 1)});
        next += word_count;
    }
}

std::uint32_t SpirvModule::id_bound() const noexcept {
    return words_[id_bound_word];
}

}
