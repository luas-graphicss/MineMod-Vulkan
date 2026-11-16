#pragma once

#include <cstddef>
#include <cstdint>
#include <memory>
#include <mutex>
#include <optional>
#include <span>
#include <string>
#include <string_view>
#include <vector>

#include "cache/shader_cache.hpp"
#include "compiler/shader_compiler.hpp"
#include "source/include_expansion.hpp"

namespace minemod::shaders {

struct ServiceLimits {
    SourceLimits source;
    std::size_t cache_bytes;
};

struct CoreShaderSource {
    std::string_view name;
    std::string_view vertex;
    std::string_view fragment;
};

struct CompiledProgram {
    std::vector<std::uint32_t> vertex;
    std::vector<std::uint32_t> fragment;
};

struct ProgramCompilation {
    std::optional<CompiledProgram> program;
    std::vector<std::string> problems;
};

class ShaderService {
public:
    ShaderService(std::unique_ptr<ShaderCompiler> compiler, const ServiceLimits& limits);

    ProgramCompilation compile_core_shader(const CoreShaderSource& source, const IncludeSources& includes);

    std::uint64_t cache_hits() const;

private:
    std::vector<std::uint32_t> compiled(
        Stage stage, const std::string& source, std::span<const std::string> files, std::vector<std::string>& problems);

    std::unique_ptr<ShaderCompiler> compiler_;
    SourceLimits source_limits_;
    mutable std::mutex mutex_;
    ShaderCache cache_;
    std::uint64_t cache_hits_ = 0;
};

}
