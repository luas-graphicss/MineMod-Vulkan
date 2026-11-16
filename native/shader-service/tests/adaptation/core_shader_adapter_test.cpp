#include <string>
#include <utility>
#include <vector>

#include <doctest.h>

#include "adaptation/core_shader_adapter.hpp"
#include "adaptation/core_shader_bindings.hpp"
#include "source/include_expansion.hpp"
#include "support/core_shader_samples.hpp"

namespace {

using namespace minemod::shaders;
using namespace minemod::shaders::test_support;

const std::string plain_vertex = "#version 150\nin vec3 Position;\nvoid main() { gl_Position = vec4(Position, 1.0); }\n";
const std::string plain_fragment = "#version 150\nout vec4 fragColor;\nvoid main() { fragColor = vec4(1.0); }\n";

ExpandedSource vertex_source(std::string_view text) {
    return ExpandedSource{std::string(text), {"entity.vsh"}};
}

ExpandedSource fragment_source(std::string_view text) {
    return ExpandedSource{std::string(text), {"entity.fsh"}};
}

Adaptation adapted(std::string_view vertex, std::string_view fragment) {
    return adapt_core_shader(scene_bindings(), vertex_source(vertex), fragment_source(fragment));
}

AdaptedProgram program_of(std::string_view vertex, std::string_view fragment) {
    Adaptation adaptation = adapted(vertex, fragment);
    REQUIRE(adaptation.problems == std::vector<std::string>{});
    return std::move(*adaptation.program);
}

std::vector<std::string> problems_of(std::string_view vertex, std::string_view fragment) {
    const Adaptation adaptation = adapted(vertex, fragment);
    CHECK_FALSE(adaptation.program.has_value());
    return adaptation.problems;
}

bool contains(const std::string& text, std::string_view part) {
    return text.find(part) != std::string::npos;
}

}

TEST_CASE("the adapted text keeps every line where it was, after the constants of the renderer") {
    const std::string vertex =
        "#version 150\n"
        "in vec3 Position;\n"
        "uniform mat4\n"
        "    ProjMat;\n"
        "void main() { gl_Position = ProjMat * vec4(Position, float(gl_VertexID)); }\n";

    const AdaptedProgram program = program_of(vertex, plain_fragment);

    const std::string blocks(scene_bindings().constant_blocks);
    CHECK(
        program.vertex
        == "#version 450\n" + blocks
            + "#define ProjMat (minemod_view.projection)\n"
              "#line 2 0\n"
              "layout(location = 0) in vec3 Position;\n"
              "\n"
              "\n"
              "void main() { gl_Position = ProjMat * vec4(Position, float(gl_VertexIndex)); }\n");
    CHECK(
        program.fragment
        == "#version 450\n" + blocks
            + "#line 2 0\n"
              "layout(location = 0) out vec4 fragColor;\n"
              "void main() { fragColor = vec4(1.0); }\n");
}

TEST_CASE("vertex elements take the locations of the vertex format, whatever their order") {
    const AdaptedProgram program = program_of(tinted_texture_vertex, tinted_texture_fragment);

    CHECK(contains(program.vertex, "layout(location = 0) in vec3 Position;"));
    CHECK(contains(program.vertex, "layout(location = 2) in vec2 UV0;"));
    CHECK(contains(program.vertex, "layout(location = 1) in vec4 Color;"));
}

TEST_CASE("uniforms of the game read the constants of the renderer") {
    const AdaptedProgram program = program_of(lit_entity_vertex, lit_entity_fragment);

    CHECK(contains(program.vertex, "#define ModelViewMat (minemod_draw.model_view)\n"));
    CHECK(contains(program.vertex, "#define FogShape (int(minemod_view.fog_shape))\n"));
    CHECK(contains(program.vertex, "#define Light1_Direction (minemod_view.light_directions[1].xyz)\n"));
    CHECK(contains(program.fragment, "#define FogColor (minemod_view.fog_color)\n"));
    CHECK_FALSE(contains(program.vertex, "uniform mat4 ProjMat"));
}

TEST_CASE("samplers are bound in the stage the renderer binds them for") {
    const AdaptedProgram program = program_of(lit_entity_vertex, lit_entity_fragment);

    CHECK(contains(program.vertex, "layout(set = 0, binding = 2) uniform sampler2D Sampler1;"));
    CHECK(contains(program.vertex, "layout(set = 0, binding = 1) uniform sampler2D Sampler2;"));
    CHECK(contains(program.fragment, "layout(set = 1, binding = 0) uniform sampler2D Sampler0;"));
}

TEST_CASE("a value written by the vertex stage is read at the same location, with its interpolation") {
    const AdaptedProgram program = program_of(lit_entity_vertex, lit_entity_fragment);

    CHECK(contains(program.vertex, "layout(location = 0) out float eyeDistance;"));
    CHECK(contains(program.fragment, "layout(location = 0) in float eyeDistance;"));
    CHECK(contains(program.vertex, "layout(location = 5) flat out int vertexParity;"));
    CHECK(contains(program.fragment, "layout(location = 5) flat in int vertexParity;"));
    CHECK(contains(program.fragment, "layout(location = 0) out vec4 fragColor;"));
}

TEST_CASE("a declaration the renderer cannot serve is dropped while nothing uses it") {
    const AdaptedProgram program = program_of(lit_entity_vertex, lit_entity_fragment);

    CHECK_FALSE(contains(program.fragment, "GameTime"));
}

TEST_CASE("a uniform, a vertex element or a sampler the renderer cannot serve is a problem once it is used") {
    const std::string timed_vertex =
        "#version 150\nin vec3 Position;\nuniform float GameTime;\n"
        "void main() { gl_Position = vec4(Position, GameTime); }\n";
    const std::string tangent_vertex =
        "#version 150\nin vec3 Position;\nin vec4 Tangent;\nvoid main() { gl_Position = vec4(Position, Tangent.w); }\n";
    const std::string sampling_vertex =
        "#version 150\nin vec3 Position;\nuniform sampler2D Sampler0;\n"
        "void main() { gl_Position = texture(Sampler0, Position.xy); }\n";

    CHECK(
        problems_of(timed_vertex, plain_fragment)
        == std::vector<std::string>{"entity.vsh:3: uses the uniform 'GameTime', which the renderer does not provide"});
    CHECK(
        problems_of(tangent_vertex, plain_fragment)
        == std::vector<std::string>{
            "entity.vsh:3: reads the vertex element 'Tangent', which no vertex format provides"});
    CHECK(
        problems_of(sampling_vertex, plain_fragment)
        == std::vector<std::string>{"entity.vsh:3: samples 'Sampler0', which is not bound for the vertex stage"});
}

TEST_CASE("a declaration whose type differs from what the renderer provides is a problem") {
    const std::string float_light = "#version 150\nin vec3 Position;\nin vec2 UV2;\nvoid main() {}\n";
    const std::string narrow_matrix = "#version 150\nuniform mat3 ProjMat;\nvoid main() {}\n";
    const std::string cube_sampler =
        "#version 150\nuniform samplerCube Sampler0;\nout vec4 fragColor;\nvoid main() { fragColor = vec4(1.0); }\n";

    CHECK(
        problems_of(float_light, plain_fragment)
        == std::vector<std::string>{"entity.vsh:3: declares 'UV2' as vec2, but the vertex format provides ivec2"});
    CHECK(
        problems_of(narrow_matrix, plain_fragment)
        == std::vector<std::string>{"entity.vsh:2: declares 'ProjMat' as mat3, but the renderer provides mat4"});
    CHECK(
        problems_of(plain_vertex, cube_sampler)
        == std::vector<std::string>{
            "entity.fsh:2: declares 'Sampler0' as samplerCube, but the renderer binds a sampler2D"});
}

TEST_CASE("layouts, blocks, arrays, initializers and shared statements are not part of the contract") {
    const std::string vertex =
        "#version 150\n"
        "layout(location = 7) in vec3 Position;\n"
        "uniform Globals { mat4 view; } globals;\n"
        "uniform float weights[4];\n"
        "uniform float scale = 2.0;\n"
        "out vec2 first, second;\n"
        "void main() {}\n";

    CHECK(
        problems_of(vertex, plain_fragment)
        == std::vector<std::string>{
            "entity.vsh:2: 'Position' declares its own layout, which a core shader replacement cannot do",
            "entity.vsh:3: 'globals' is an interface block, which a core shader replacement cannot do",
            "entity.vsh:4: 'weights' is an array, which a core shader replacement cannot do",
            "entity.vsh:5: 'scale' has an initializer, which a core shader replacement cannot do",
            "entity.vsh:6: 'first' shares its statement with other names, which a core shader replacement cannot do",
        });
}

TEST_CASE("the fragment stage reads only what the vertex stage writes, with the same type") {
    const std::string vertex =
        "#version 150\nin vec3 Position;\nout vec3 glow;\nvoid main() { glow = Position; gl_Position = vec4(1.0); }\n";
    const std::string unwritten =
        "#version 150\nin vec4 shine;\nout vec4 fragColor;\nvoid main() { fragColor = shine; }\n";
    const std::string retyped =
        "#version 150\nin vec4 glow;\nout vec4 fragColor;\nvoid main() { fragColor = glow; }\n";

    CHECK(
        problems_of(vertex, unwritten)
        == std::vector<std::string>{"entity.fsh:2: reads 'shine', which the vertex shader does not write"});
    CHECK(
        problems_of(vertex, retyped)
        == std::vector<std::string>{"entity.fsh:2: reads 'glow' as vec4, but the vertex shader writes vec3"});
}

TEST_CASE("the fragment stage writes exactly one vec4 color") {
    const std::string none = "#version 150\nvoid main() {}\n";
    const std::string two =
        "#version 150\nout vec4 fragColor;\nout vec4 brightColor;\nvoid main() { fragColor = brightColor = vec4(1.0); }\n";
    const std::string narrow = "#version 150\nout vec3 fragColor;\nvoid main() { fragColor = vec3(1.0); }\n";

    CHECK(
        problems_of(plain_vertex, none)
        == std::vector<std::string>{"entity.fsh: the fragment shader writes no color output"});
    CHECK(
        problems_of(plain_vertex, two)
        == std::vector<std::string>{"entity.fsh:3: writes 'brightColor' besides the one color output of the pass"});
    CHECK(
        problems_of(plain_vertex, narrow)
        == std::vector<std::string>{"entity.fsh:2: declares the color output 'fragColor' as vec3"});
}

TEST_CASE("names of the renderer, a missing version and an old version are refused") {
    const std::string reserved =
        "#version 150\nin vec3 Position;\nvoid main() { float minemod_draw_copy = 1.0; gl_Position = vec4(Position, 1.0); }\n";
    const std::string unversioned = "in vec3 Position;\nvoid main() {}\n";
    const std::string legacy = "#version 120\nvoid main() {}\n";

    CHECK(
        problems_of(reserved, plain_fragment)
        == std::vector<std::string>{
            "entity.vsh:3: uses the name 'minemod_draw_copy', which is reserved for the renderer"});
    CHECK(
        problems_of(unversioned, plain_fragment)
        == std::vector<std::string>{"entity.vsh:1: the shader does not start with a #version directive"});
    CHECK(
        problems_of(legacy, plain_fragment)
        == std::vector<std::string>{"entity.vsh:1: GLSL 120 is outside the versions 150 to 460"});
}

TEST_CASE("a problem inside an imported file is reported at the line of that file") {
    const IncludeSources includes{{"<time.glsl>", "\nuniform float GameTime;\nfloat now() { return GameTime; }\n"}};
    const ExpandedSource vertex = expand_includes(
        "entity.vsh", "#version 150\n#moj_import <time.glsl>\nvoid main() { gl_Position = vec4(now()); }\n", includes);

    const Adaptation adaptation = adapt_core_shader(scene_bindings(), vertex, fragment_source(plain_fragment));

    CHECK(
        adaptation.problems
        == std::vector<std::string>{
            "<time.glsl>:2: uses the uniform 'GameTime', which the renderer does not provide"});
}

TEST_CASE("text that is not GLSL is reported once, at its line") {
    CHECK(
        problems_of("#version 150\n/* never closed\nvoid main() {}\n", plain_fragment)
        == std::vector<std::string>{"entity.vsh:2: a block comment is never closed"});
}
