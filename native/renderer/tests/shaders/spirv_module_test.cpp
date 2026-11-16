#include <stdint.h>

#include <array>
#include <cstdint>
#include <vector>

#include <doctest.h>

#include "shaders/spirv_module.hpp"
#include "sprite_vert.h"

namespace {

using namespace minemod::renderer;

constexpr std::array<std::uint32_t, 5> empty_module{0x07230203, 0x00010600, 0, 1, 0};

std::vector<std::uint32_t> sprite_words() {
    return std::vector<std::uint32_t>(std::begin(sprite_vert), std::end(sprite_vert));
}

}

TEST_CASE("a compiled shader is accepted and split into its instructions") {
    const SpirvModule module{sprite_vert};

    CHECK(module.words().size() == std::size(sprite_vert));
    CHECK(module.id_bound() > 0);
    CHECK_FALSE(module.instructions().empty());
}

TEST_CASE("a module without instructions is accepted") {
    const SpirvModule module{empty_module};

    CHECK(module.instructions().empty());
}

TEST_CASE("a module shorter than its header is rejected") {
    const std::array<std::uint32_t, 4> truncated_header{0x07230203, 0x00010600, 0, 1};

    CHECK_THROWS_AS(SpirvModule{truncated_header}, ShaderError);
}

TEST_CASE("a module with another magic number, version, id bound or schema is rejected") {
    std::array<std::uint32_t, 5> byte_swapped_magic = empty_module;
    byte_swapped_magic[0] = 0x03022307;
    std::array<std::uint32_t, 5> future_version = empty_module;
    future_version[1] = 0x00020000;
    std::array<std::uint32_t, 5> no_ids = empty_module;
    no_ids[3] = 0;
    std::array<std::uint32_t, 5> reserved_schema = empty_module;
    reserved_schema[4] = 1;

    CHECK_THROWS_AS(SpirvModule{byte_swapped_magic}, ShaderError);
    CHECK_THROWS_AS(SpirvModule{future_version}, ShaderError);
    CHECK_THROWS_AS(SpirvModule{no_ids}, ShaderError);
    CHECK_THROWS_AS(SpirvModule{reserved_schema}, ShaderError);
}

TEST_CASE("an instruction that runs past the end of the module is rejected") {
    const std::array<std::uint32_t, 6> missing_operand{0x07230203, 0x00010600, 0, 1, 0, (2u << 16) | 17u};

    CHECK_THROWS_AS(SpirvModule{missing_operand}, ShaderError);
}

TEST_CASE("an instruction that claims no words is rejected") {
    std::vector<std::uint32_t> padded = sprite_words();
    padded.push_back(0);

    CHECK_THROWS_AS(SpirvModule{padded}, ShaderError);
}

TEST_CASE("a module above the size or id limits is rejected") {
    const std::vector<std::uint32_t> words = sprite_words();

    CHECK_THROWS_AS(SpirvModule(words, SpirvLimits{words.size() - 1, 1u << 16}), ShaderError);
    CHECK_THROWS_AS(SpirvModule(words, SpirvLimits{words.size(), 1}), ShaderError);
}
