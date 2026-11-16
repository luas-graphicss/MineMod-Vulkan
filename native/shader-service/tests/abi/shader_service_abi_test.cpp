#include <cstdint>
#include <string>
#include <vector>

#include <doctest.h>

#include "minemod/shaders/shader_service.h"
#include "support/core_shader_samples.hpp"
#include "support/scratch_directory.hpp"

namespace {

using namespace minemod::shaders::test_support;

constexpr std::uint32_t spirv_magic = 0x07230203;

minemod_shader_text text_of(std::string_view text) {
    return minemod_shader_text{text.data(), text.size()};
}

minemod_shader_service_config config_for(const char* compiler, const char* scratch) {
    return minemod_shader_service_config{
        .struct_bytes = sizeof(minemod_shader_service_config),
        .abi_version = {MINEMOD_ABI_VERSION_MAJOR, MINEMOD_ABI_VERSION_MINOR},
        .wall_time_milliseconds = 20000,
        .reserved = 0,
        .compiler_memory_bytes = std::uint64_t{1} << 30,
        .cache_bytes = std::uint64_t{1} << 20,
        .compiler_path = compiler,
        .scratch_directory = scratch,
    };
}

minemod_core_shader_source source_of(std::string_view name, std::string_view vertex, std::string_view fragment) {
    return minemod_core_shader_source{
        .struct_bytes = sizeof(minemod_core_shader_source),
        .include_count = 0,
        .name = text_of(name),
        .vertex = text_of(vertex),
        .fragment = text_of(fragment),
        .includes = nullptr,
    };
}

class OpenService {
public:
    OpenService() {
        const std::string scratch = scratch_.path().string();
        const minemod_shader_service_config config = config_for(MINEMOD_GLSLANG_VALIDATOR, scratch.c_str());
        REQUIRE(minemod_shader_service_create(&config, &service_) == MINEMOD_RESULT_OK);
    }

    ~OpenService() {
        minemod_shader_service_destroy(service_);
    }

    OpenService(const OpenService&) = delete;
    OpenService& operator=(const OpenService&) = delete;

    minemod_shader_service* get() const noexcept {
        return service_;
    }

private:
    ScratchDirectory scratch_;
    minemod_shader_service* service_ = nullptr;
};

std::vector<std::uint32_t> words_of(const minemod_shader_compilation* compilation, std::uint32_t stage) {
    std::vector<std::uint32_t> words(minemod_shader_compilation_read_words(compilation, stage, nullptr, 0));
    minemod_shader_compilation_read_words(compilation, stage, words.data(), words.size());
    return words;
}

std::string text_of(const minemod_shader_compilation* compilation) {
    std::string text(minemod_shader_compilation_read_text(compilation, nullptr, 0), '\0');
    minemod_shader_compilation_read_text(compilation, text.data(), text.size());
    return text;
}

}

TEST_CASE("a service compiles a replacement and hands out the words of both stages") {
    const OpenService service;
    const minemod_core_shader_source source =
        source_of("tinted_texture", tinted_texture_vertex, tinted_texture_fragment);
    minemod_shader_compilation* compilation = nullptr;

    REQUIRE(minemod_shader_service_compile_core_shader(service.get(), &source, &compilation) == MINEMOD_RESULT_OK);

    CHECK(minemod_shader_compilation_succeeded(compilation) == 1);
    CHECK(words_of(compilation, MINEMOD_SHADER_STAGE_VERTEX).front() == spirv_magic);
    CHECK(words_of(compilation, MINEMOD_SHADER_STAGE_FRAGMENT).front() == spirv_magic);
    CHECK(text_of(compilation).empty());
    CHECK(minemod_shader_compilation_read_words(compilation, 2, nullptr, 0) == 0);
    minemod_shader_compilation_destroy(compilation);
}

TEST_CASE("imports are resolved from the sources that come with the request") {
    const OpenService service;
    const std::string key = "<shade.glsl>";
    const minemod_shader_include include{text_of(key), text_of(shade_include)};
    minemod_core_shader_source source = source_of("lit_entity", lit_entity_vertex, lit_entity_fragment);
    source.include_count = 1;
    source.includes = &include;
    minemod_shader_compilation* compilation = nullptr;

    REQUIRE(minemod_shader_service_compile_core_shader(service.get(), &source, &compilation) == MINEMOD_RESULT_OK);

    CHECK(text_of(compilation) == "");
    CHECK(minemod_shader_compilation_succeeded(compilation) == 1);
    minemod_shader_compilation_destroy(compilation);
}

TEST_CASE("the problems of a refused replacement are read as text and no words are handed out") {
    const OpenService service;
    const std::string vertex =
        "#version 150\nin vec3 Position;\nuniform float GameTime;\n"
        "void main() { gl_Position = vec4(Position, GameTime); }\n";
    const std::string fragment = "#version 150\nout vec4 fragColor;\nvoid main() { fragColor = vec4(1.0); }\n";
    const minemod_core_shader_source source = source_of("timed", vertex, fragment);
    minemod_shader_compilation* compilation = nullptr;

    REQUIRE(minemod_shader_service_compile_core_shader(service.get(), &source, &compilation) == MINEMOD_RESULT_OK);

    CHECK(minemod_shader_compilation_succeeded(compilation) == 0);
    CHECK(text_of(compilation) == "timed.vsh:3: uses the uniform 'GameTime', which the renderer does not provide\n");
    CHECK(minemod_shader_compilation_read_words(compilation, MINEMOD_SHADER_STAGE_VERTEX, nullptr, 0) == 0);
    minemod_shader_compilation_destroy(compilation);
}

TEST_CASE("a configuration outside the limits, a relative path and another program are refused") {
    const ScratchDirectory scratch;
    const std::string scratch_path = scratch.path().string();
    minemod_shader_service* service = nullptr;
    minemod_shader_service_config brief = config_for(MINEMOD_GLSLANG_VALIDATOR, scratch_path.c_str());
    brief.wall_time_milliseconds = 10;
    minemod_shader_service_config resized = config_for(MINEMOD_GLSLANG_VALIDATOR, scratch_path.c_str());
    resized.struct_bytes -= 1;
    const minemod_shader_service_config relative = config_for("glslangValidator", scratch_path.c_str());
    const minemod_shader_service_config other_program = config_for("/bin/true", scratch_path.c_str());
    const minemod_shader_service_config valid = config_for(MINEMOD_GLSLANG_VALIDATOR, scratch_path.c_str());

    CHECK(minemod_shader_service_create(&brief, &service) == MINEMOD_RESULT_LIMIT_EXCEEDED);
    CHECK(minemod_shader_service_create(&resized, &service) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_shader_service_create(&relative, &service) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_shader_service_create(&other_program, &service) == MINEMOD_RESULT_UNSUPPORTED_CAPABILITY);
    CHECK(minemod_shader_service_create(nullptr, &service) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_shader_service_create(&valid, nullptr) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(service == nullptr);
}

TEST_CASE("sources beyond the limits and names that are not printable are refused") {
    const OpenService service;
    minemod_shader_compilation* compilation = nullptr;
    minemod_core_shader_source oversized = source_of("tinted_texture", tinted_texture_vertex, tinted_texture_fragment);
    oversized.vertex.length = MINEMOD_SHADER_MAX_SOURCE_BYTES + 1;
    minemod_core_shader_source crowded = source_of("tinted_texture", tinted_texture_vertex, tinted_texture_fragment);
    crowded.include_count = MINEMOD_SHADER_MAX_INCLUDES + 1;
    const minemod_core_shader_source unnamed = source_of("", tinted_texture_vertex, tinted_texture_fragment);
    const minemod_core_shader_source two_lines =
        source_of("tinted\ntexture", tinted_texture_vertex, tinted_texture_fragment);

    CHECK(
        minemod_shader_service_compile_core_shader(service.get(), &oversized, &compilation)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(
        minemod_shader_service_compile_core_shader(service.get(), &crowded, &compilation)
        == MINEMOD_RESULT_LIMIT_EXCEEDED);
    CHECK(
        minemod_shader_service_compile_core_shader(service.get(), &unnamed, &compilation)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(
        minemod_shader_service_compile_core_shader(service.get(), &two_lines, &compilation)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(compilation == nullptr);
}
