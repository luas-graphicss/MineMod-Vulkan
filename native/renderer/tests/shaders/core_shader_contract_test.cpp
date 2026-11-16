#include <stdint.h>

#include <string>
#include <vector>

#include <doctest.h>

#include "shaders/core_shader_contract.hpp"
#include "shaders/shader_interface.hpp"
#include "shaders/spirv_module.hpp"
#include "sprite_frag.h"
#include "sprite_vert.h"

namespace {

using namespace minemod::renderer;

CoreShaderContract sprite_contract() {
    return CoreShaderContract{"sprite", {0, 1, 2}, {{0, 0}}, {0}};
}

ShaderInterface sprite_vertex() {
    return reflect_interface(SpirvModule{sprite_vert});
}

ShaderInterface sprite_fragment() {
    return reflect_interface(SpirvModule{sprite_frag});
}

}

TEST_CASE("shaders that stay inside the contract are accepted") {
    CHECK(contract_violations(sprite_contract(), sprite_vertex(), sprite_fragment()).empty());
}

TEST_CASE("a vertex attribute the format does not provide is reported") {
    CoreShaderContract without_tint = sprite_contract();
    without_tint.vertex_locations = {0, 1};

    const std::vector<std::string> violations = contract_violations(without_tint, sprite_vertex(), sprite_fragment());

    REQUIRE(violations.size() == 1);
    CHECK(
        violations.front()
        == "core shader 'sprite': the vertex shader reads location 2, which the vertex format does not provide");
}

TEST_CASE("a resource the renderer does not bind is reported") {
    CoreShaderContract without_atlas = sprite_contract();
    without_atlas.resources.clear();

    const std::vector<std::string> violations = contract_violations(without_atlas, sprite_vertex(), sprite_fragment());

    REQUIRE(violations.size() == 1);
    CHECK(violations.front() == "core shader 'sprite': the fragment shader uses set 0 binding 0, which is not bound");
}

TEST_CASE("color outputs must match the attachments of the pass") {
    CoreShaderContract two_attachments = sprite_contract();
    two_attachments.color_outputs = {0, 1};
    CoreShaderContract no_attachment = sprite_contract();
    no_attachment.color_outputs.clear();

    CHECK(
        contract_violations(two_attachments, sprite_vertex(), sprite_fragment())
        == std::vector<std::string>{"core shader 'sprite': the fragment shader does not write color output 1"});
    CHECK(
        contract_violations(no_attachment, sprite_vertex(), sprite_fragment())
        == std::vector<std::string>{
            "core shader 'sprite': the fragment shader writes color output 0, which has no attachment"});
}

TEST_CASE("stages given the wrong shader and varyings nobody writes are reported") {
    const std::vector<std::string> violations =
        contract_violations(sprite_contract(), sprite_fragment(), sprite_vertex());

    REQUIRE_FALSE(violations.empty());
    CHECK(violations.front() == "core shader 'sprite': the vertex stage was given a fragment shader");
}
