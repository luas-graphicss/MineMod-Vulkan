#pragma once

#include <cstddef>
#include <filesystem>
#include <stdexcept>
#include <string>
#include <string_view>

#include "compiler/shader_compiler.hpp"
#include "process/child_process.hpp"

namespace minemod::shaders {

class CompilerUnavailable : public std::runtime_error {
public:
    using std::runtime_error::runtime_error;
};

struct CompilerSetup {
    std::filesystem::path executable;
    std::filesystem::path scratch_directory;
    ProcessLimits limits;
    std::size_t max_spirv_words;
};

class GlslangProcessCompiler final : public ShaderCompiler {
public:
    explicit GlslangProcessCompiler(CompilerSetup setup);

    Compilation compile(Stage stage, std::string_view source) override;

    std::string_view identity() const noexcept override {
        return identity_;
    }

private:
    CompilerSetup setup_;
    std::string identity_;
};

}
