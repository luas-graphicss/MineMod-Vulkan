#include <chrono>
#include <cstdint>
#include <filesystem>
#include <fstream>
#include <string>

#include <doctest.h>

#include "compiler/glslang_process_compiler.hpp"
#include "support/scratch_directory.hpp"

namespace {

using namespace minemod::shaders;
using namespace std::chrono_literals;
using minemod::shaders::test_support::ScratchDirectory;

constexpr std::uint32_t spirv_magic = 0x07230203;
constexpr std::string_view valid_vertex = "#version 450\nvoid main() { gl_Position = vec4(0.0); }\n";
const ProcessLimits compiler_limits{20000ms, 20s, std::uint64_t{1} << 30, std::uint64_t{1} << 23, 1 << 16};

CompilerSetup setup_in(const std::filesystem::path& scratch) {
    return CompilerSetup{MINEMOD_GLSLANG_VALIDATOR, scratch, compiler_limits, std::size_t{1} << 20};
}

std::filesystem::path stalling_compiler_in(const std::filesystem::path& directory) {
    const std::filesystem::path script = directory / "stalling-compiler";
    std::ofstream(script) << "#!/bin/sh\n"
                             "if [ \"$1\" = \"--version\" ]; then\n"
                             "    echo \"Glslang Version: stalling\"\n"
                             "    exit 0\n"
                             "fi\n"
                             "exec /bin/sleep 30\n";
    std::filesystem::permissions(script, std::filesystem::perms::owner_all);
    return script;
}

}

TEST_CASE("a valid shader compiles to a SPIR-V module and leaves the scratch directory empty") {
    const ScratchDirectory scratch;
    GlslangProcessCompiler compiler(setup_in(scratch.path()));

    const Compilation compilation = compiler.compile(Stage::vertex, valid_vertex);

    REQUIRE_FALSE(compilation.spirv.empty());
    CHECK(compilation.spirv.front() == spirv_magic);
    CHECK(std::filesystem::is_empty(scratch.path()));
}

TEST_CASE("a shader with an error returns the log of the compiler and no module") {
    const ScratchDirectory scratch;
    GlslangProcessCompiler compiler(setup_in(scratch.path()));

    const Compilation compilation =
        compiler.compile(Stage::fragment, "#version 450\n#line 40 3\nvoid main() { missing = 1.0; }\n");

    CHECK(compilation.spirv.empty());
    CHECK(compilation.log.find("ERROR: 3:40: 'missing' : undeclared identifier") != std::string::npos);
}

TEST_CASE("the identity names the version of the compiler and the target it compiles for") {
    const ScratchDirectory scratch;
    const GlslangProcessCompiler compiler(setup_in(scratch.path()));

    CHECK(compiler.identity().starts_with("Glslang Version:"));
    CHECK(compiler.identity().ends_with("; target vulkan1.3"));
}

TEST_CASE("a module beyond the accepted size is refused") {
    const ScratchDirectory scratch;
    CompilerSetup setup = setup_in(scratch.path());
    setup.max_spirv_words = 16;
    GlslangProcessCompiler compiler(setup);

    const Compilation compilation = compiler.compile(Stage::vertex, valid_vertex);

    CHECK(compilation.spirv.empty());
    CHECK(compilation.log == "the shader compiler wrote no SPIR-V within 16 words");
}

TEST_CASE("a compiler that does not finish in time is stopped and reported") {
    const ScratchDirectory scratch;
    const ScratchDirectory tools;
    CompilerSetup setup = setup_in(scratch.path());
    setup.executable = stalling_compiler_in(tools.path());
    setup.limits.wall_time = 300ms;
    GlslangProcessCompiler compiler(setup);

    const Compilation compilation = compiler.compile(Stage::vertex, valid_vertex);

    CHECK(compilation.spirv.empty());
    CHECK(compilation.log == "the shader compiler did not finish within 300 ms");
    CHECK(std::filesystem::is_empty(scratch.path()));
}

TEST_CASE("a program that is not glslang, relative paths and a missing scratch directory are refused") {
    const ScratchDirectory scratch;
    CompilerSetup other_program = setup_in(scratch.path());
    other_program.executable = "/bin/true";
    CompilerSetup missing_program = setup_in(scratch.path());
    missing_program.executable = "/nonexistent/glslangValidator";
    CompilerSetup relative_program = setup_in(scratch.path());
    relative_program.executable = "glslangValidator";

    CHECK_THROWS_AS(GlslangProcessCompiler{other_program}, CompilerUnavailable);
    CHECK_THROWS_AS(GlslangProcessCompiler{missing_program}, CompilerUnavailable);
    CHECK_THROWS_AS(GlslangProcessCompiler{relative_program}, CompilerUnavailable);
    CHECK_THROWS_AS(GlslangProcessCompiler{setup_in(scratch.path() / "missing")}, CompilerUnavailable);
}
