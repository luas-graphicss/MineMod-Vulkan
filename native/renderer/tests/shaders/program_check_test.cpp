#include <stdint.h>

#include <array>
#include <cstdint>
#include <string>
#include <vector>

#include <doctest.h>

#include "scene_frag.h"
#include "scene_vert.h"
#include "shaders/program_check.hpp"
#include "shaders/scene_shader_contract.hpp"

namespace {

using namespace minemod::renderer;

constexpr std::array<std::uint32_t, 3> three_words{1, 2, 3};

}

TEST_CASE("a pair of modules inside the contract is kept for the device") {
    const ProgramCheck check = check_program(scene_shader_contract(), scene_vert, scene_frag);

    CHECK(check.violations == std::vector<std::string>{});
    REQUIRE(check.program.has_value());
    CHECK(check.program->vertex.words().size() == std::size(scene_vert));
    CHECK(check.program->fragment.words().size() == std::size(scene_frag));
}

TEST_CASE("words that are not a SPIR-V module are reported instead of thrown") {
    const ProgramCheck check = check_program(scene_shader_contract(), three_words, scene_frag);

    CHECK_FALSE(check.program.has_value());
    CHECK(
        check.violations
        == std::vector<std::string>{"core shader 'scene': the SPIR-V module is shorter than its header"});
}

TEST_CASE("modules given to the wrong stage break the contract") {
    const ProgramCheck check = check_program(scene_shader_contract(), scene_frag, scene_vert);

    CHECK_FALSE(check.program.has_value());
    REQUIRE_FALSE(check.violations.empty());
    CHECK(check.violations.front() == "core shader 'scene': the vertex stage was given a fragment shader");
}

TEST_CASE("a module beyond the accepted size is refused") {
    const SpirvLimits sixteen_words{16, std::uint32_t{1} << 16};

    const ProgramCheck check = check_program(scene_shader_contract(), scene_vert, scene_frag, sixteen_words);

    CHECK(
        check.violations
        == std::vector<std::string>{"core shader 'scene': the SPIR-V module is larger than the accepted size"});
}
