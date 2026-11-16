#include "service/shader_service.hpp"

#include <utility>

#include "adaptation/core_shader_adapter.hpp"
#include "adaptation/core_shader_bindings.hpp"
#include "compiler/compiler_log.hpp"
#include "source/source_error.hpp"

namespace minemod::shaders {

namespace {

constexpr std::string_view vertex_extension = ".vsh";
constexpr std::string_view fragment_extension = ".fsh";

std::string file_name(std::string_view shader, std::string_view extension) {
    return std::string(shader) + std::string(extension);
}

}

ShaderService::ShaderService(std::unique_ptr<ShaderCompiler> compiler, const ServiceLimits& limits)
    : compiler_(std::move(compiler)), source_limits_(limits.source), cache_(limits.cache_bytes) {}

ProgramCompilation ShaderService::compile_core_shader(const CoreShaderSource& source, const IncludeSources& includes) {
    ProgramCompilation compilation;
    try {
        const ExpandedSource vertex =
            expand_includes(file_name(source.name, vertex_extension), source.vertex, includes, source_limits_);
        const ExpandedSource fragment =
            expand_includes(file_name(source.name, fragment_extension), source.fragment, includes, source_limits_);
        Adaptation adaptation = adapt_core_shader(scene_bindings(), vertex, fragment);
        if (!adaptation.program) {
            compilation.problems = std::move(adaptation.problems);
            return compilation;
        }
        std::vector<std::uint32_t> vertex_words =
            compiled(Stage::vertex, adaptation.program->vertex, vertex.files, compilation.problems);
        std::vector<std::uint32_t> fragment_words =
            compiled(Stage::fragment, adaptation.program->fragment, fragment.files, compilation.problems);
        if (compilation.problems.empty()) {
            compilation.program = CompiledProgram{std::move(vertex_words), std::move(fragment_words)};
        }
    } catch (const SourceError& error) {
        compilation.problems.emplace_back(error.what());
    }
    return compilation;
}

std::uint64_t ShaderService::cache_hits() const {
    const std::scoped_lock lock(mutex_);
    return cache_hits_;
}

std::vector<std::uint32_t> ShaderService::compiled(
    Stage stage, const std::string& source, std::span<const std::string> files, std::vector<std::string>& problems) {
    std::string key = shader_cache_key(stage, compiler_->identity(), scene_bindings(), source);
    {
        const std::scoped_lock lock(mutex_);
        if (std::optional<std::vector<std::uint32_t>> cached = cache_.find(key)) {
            ++cache_hits_;
            return std::move(*cached);
        }
    }
    Compilation compilation = compiler_->compile(stage, source);
    if (compilation.spirv.empty()) {
        std::vector<std::string> errors = located_errors(compilation.log, files);
        problems.insert(
            problems.end(), std::make_move_iterator(errors.begin()), std::make_move_iterator(errors.end()));
        return {};
    }
    const std::scoped_lock lock(mutex_);
    cache_.store(std::move(key), compilation.spirv);
    return std::move(compilation.spirv);
}

}
