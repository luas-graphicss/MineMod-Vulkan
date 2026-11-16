#include <chrono>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <iterator>
#include <memory>
#include <string>
#include <vector>

#include <doctest.h>

#include "adaptation/core_shader_bindings.hpp"
#include "compiler/glslang_process_compiler.hpp"
#include "service/shader_service.hpp"
#include "shaders/core_shader_contract.hpp"
#include "shaders/scene_shader_contract.hpp"
#include "shaders/shader_interface.hpp"
#include "shaders/spirv_module.hpp"
#include "support/core_shader_samples.hpp"
#include "support/scratch_directory.hpp"

namespace {

using namespace minemod::shaders;
using namespace minemod::shaders::test_support;
using namespace std::chrono_literals;
namespace renderer = minemod::renderer;

const ProcessLimits compiler_limits{20000ms, 20s, std::uint64_t{1} << 30, std::uint64_t{1} << 23, 1 << 16};

class RunningService {
public:
    RunningService()
        : service_(
              std::make_unique<GlslangProcessCompiler>(
                  CompilerSetup{MINEMOD_GLSLANG_VALIDATOR, scratch_.path(), compiler_limits, std::size_t{1} << 20}),
              ServiceLimits{default_source_limits, std::size_t{1} << 20}) {}

    ShaderService& operator*() noexcept {
        return service_;
    }

    ShaderService* operator->() noexcept {
        return &service_;
    }

private:
    ScratchDirectory scratch_;
    ShaderService service_;
};

std::vector<std::string> interface_violations(const CompiledProgram& program) {
    const renderer::ShaderInterface vertex = renderer::reflect_interface(renderer::SpirvModule{program.vertex});
    const renderer::ShaderInterface fragment = renderer::reflect_interface(renderer::SpirvModule{program.fragment});
    return renderer::contract_violations(renderer::scene_shader_contract(), vertex, fragment);
}

std::string block_body(const std::string& text, const std::string& block) {
    const std::size_t declared = text.find("uniform " + block + " {");
    REQUIRE(declared != std::string::npos);
    const std::size_t opened = text.find('{', declared);
    return text.substr(opened, text.find('}', opened) - opened);
}

std::string scene_vertex_shader() {
    std::ifstream file(MINEMOD_SCENE_VERTEX_SHADER);
    REQUIRE(file.is_open());
    return std::string(std::istreambuf_iterator<char>(file), std::istreambuf_iterator<char>());
}

}

TEST_CASE("a lit entity replacement compiles into modules that keep the scene contract") {
    RunningService service;

    const ProgramCompilation compilation =
        service->compile_core_shader({"lit_entity", lit_entity_vertex, lit_entity_fragment}, sample_includes());

    REQUIRE(compilation.problems == std::vector<std::string>{});
    REQUIRE(compilation.program.has_value());
    CHECK(interface_violations(*compilation.program) == std::vector<std::string>{});
    const renderer::ShaderInterface vertex =
        renderer::reflect_interface(renderer::SpirvModule{compilation.program->vertex});
    CHECK(vertex.input_locations == std::vector<std::uint32_t>{0, 1, 2, 3, 4, 5});
}

TEST_CASE("a replacement that declares three vertex elements reads only those") {
    RunningService service;

    const ProgramCompilation compilation =
        service->compile_core_shader({"tinted_texture", tinted_texture_vertex, tinted_texture_fragment}, {});

    REQUIRE(compilation.program.has_value());
    CHECK(interface_violations(*compilation.program) == std::vector<std::string>{});
    const renderer::ShaderInterface vertex =
        renderer::reflect_interface(renderer::SpirvModule{compilation.program->vertex});
    CHECK(vertex.input_locations == std::vector<std::uint32_t>{0, 1, 2});
}

TEST_CASE("compiling a replacement again is answered by the cache") {
    RunningService service;
    const CoreShaderSource source{"tinted_texture", tinted_texture_vertex, tinted_texture_fragment};
    const ProgramCompilation first = service->compile_core_shader(source, {});
    REQUIRE(first.program.has_value());
    REQUIRE(service->cache_hits() == 0);

    const ProgramCompilation second = service->compile_core_shader(source, {});

    REQUIRE(second.program.has_value());
    CHECK(service->cache_hits() == 2);
    CHECK(second.program->vertex == first.program->vertex);
    CHECK(second.program->fragment == first.program->fragment);
}

TEST_CASE("an error of the compiler is reported at the line of the pack file") {
    RunningService service;
    const std::string fragment =
        "#version 150\n\nuniform sampler2D Sampler0;\n\nin vec2 surfaceCoordinate;\nin vec4 tint;\n\n"
        "out vec4 fragColor;\n\nvoid main() {\n    fragColor = texture(Sampler0, surfaceCoordinate) * glow;\n}\n";

    const ProgramCompilation compilation =
        service->compile_core_shader({"tinted_texture", tinted_texture_vertex, fragment}, {});

    CHECK_FALSE(compilation.program.has_value());
    REQUIRE_FALSE(compilation.problems.empty());
    CHECK(compilation.problems.front() == "tinted_texture.fsh:11: 'glow' : undeclared identifier");
}

TEST_CASE("an error inside an imported file names that file") {
    RunningService service;
    const IncludeSources includes{{"<shade.glsl>", "#version 150\n\nvec4 shaded(vec3 a, vec3 b, vec3 c, vec4 d) {\n    return missing;\n}\n"}};

    const ProgramCompilation compilation =
        service->compile_core_shader({"lit_entity", lit_entity_vertex, lit_entity_fragment}, includes);

    REQUIRE_FALSE(compilation.problems.empty());
    CHECK(compilation.problems.front() == "<shade.glsl>:4: 'missing' : undeclared identifier");
}

TEST_CASE("a replacement outside the contract and an import the packs lack are refused with the reason") {
    RunningService service;
    const std::string timed_vertex =
        "#version 150\nin vec3 Position;\nuniform float GameTime;\n"
        "void main() { gl_Position = vec4(Position, GameTime); }\n";

    const ProgramCompilation outside =
        service->compile_core_shader({"timed", timed_vertex, tinted_texture_fragment}, {});
    const ProgramCompilation unresolved =
        service->compile_core_shader({"lit_entity", lit_entity_vertex, lit_entity_fragment}, {});

    CHECK_FALSE(outside.program.has_value());
    CHECK(outside.problems.front() == "timed.vsh:3: uses the uniform 'GameTime', which the renderer does not provide");
    CHECK(
        unresolved.problems
        == std::vector<std::string>{"lit_entity.vsh:3: <shade.glsl> is not provided by the resource packs"});
}

TEST_CASE("the constant blocks given to replacements are the ones of the scene shaders") {
    const std::string scene = scene_vertex_shader();
    const std::string blocks(scene_bindings().constant_blocks);

    CHECK(block_body(blocks, "MinemodDraw") == block_body(scene, "Draw"));
    CHECK(block_body(blocks, "MinemodView") == block_body(scene, "View"));
}
