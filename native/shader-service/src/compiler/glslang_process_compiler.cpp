#include "compiler/glslang_process_compiler.hpp"

#include <algorithm>
#include <array>
#include <bit>
#include <cstdint>
#include <cstdlib>
#include <fstream>
#include <system_error>
#include <utility>
#include <vector>

namespace minemod::shaders {

namespace {

static_assert(std::endian::native == std::endian::little, "SPIR-V files are read as little-endian words");

constexpr std::string_view target_environment = "vulkan1.3";
constexpr std::string_view version_marker = "Glslang Version:";

class ScratchDirectory {
public:
    explicit ScratchDirectory(const std::filesystem::path& parent) {
        std::string pattern = (parent / "compile-XXXXXX").string();
        if (::mkdtemp(pattern.data()) == nullptr) {
            throw ProcessError("the shader compiler has no scratch directory to write into");
        }
        path_ = pattern;
    }

    ScratchDirectory(const ScratchDirectory&) = delete;
    ScratchDirectory& operator=(const ScratchDirectory&) = delete;

    ~ScratchDirectory() {
        std::error_code removal;
        std::filesystem::remove_all(path_, removal);
    }

    const std::filesystem::path& path() const noexcept {
        return path_;
    }

private:
    std::filesystem::path path_;
};

void write_source(const std::filesystem::path& file, std::string_view source) {
    std::ofstream output(file, std::ios::binary);
    output.write(source.data(), static_cast<std::streamsize>(source.size()));
    output.close();
    if (!output) {
        throw ProcessError("the shader source could not be handed to the compiler");
    }
}

std::vector<std::uint32_t> read_spirv(const std::filesystem::path& file, std::size_t max_words) {
    std::error_code error;
    const std::uintmax_t bytes = std::filesystem::file_size(file, error);
    const bool is_whole_words = !error && bytes != 0 && bytes % sizeof(std::uint32_t) == 0;
    if (!is_whole_words || bytes / sizeof(std::uint32_t) > max_words) {
        return {};
    }
    std::vector<std::uint32_t> words(static_cast<std::size_t>(bytes / sizeof(std::uint32_t)));
    std::ifstream input(file, std::ios::binary);
    input.read(reinterpret_cast<char*>(words.data()), static_cast<std::streamsize>(bytes));
    if (input.gcount() != static_cast<std::streamsize>(bytes)) {
        return {};
    }
    return words;
}

std::string version_line(std::string_view output) {
    const std::size_t start = output.find(version_marker);
    if (start == std::string_view::npos) {
        return {};
    }
    const std::size_t end = std::min(output.find('\n', start), output.size());
    return std::string(output.substr(start, end - start));
}

std::string probed_version(const CompilerSetup& setup) {
    const std::string name = setup.executable.filename().string();
    const std::array<std::string, 1> arguments{"--version"};
    try {
        const ProcessResult probe = run_process(setup.executable, arguments, setup.limits);
        const std::string version = version_line(probe.output);
        if (probe.end != ProcessEnd::exited || probe.exit_code != 0 || version.empty()) {
            throw CompilerUnavailable(name + " did not answer as a glslang compiler");
        }
        return version;
    } catch (const ProcessError& error) {
        throw CompilerUnavailable(name + " could not be asked for its version: " + error.what());
    }
}

}

GlslangProcessCompiler::GlslangProcessCompiler(CompilerSetup setup) : setup_(std::move(setup)) {
    if (!setup_.executable.is_absolute() || !setup_.scratch_directory.is_absolute()) {
        throw CompilerUnavailable("the shader compiler and its scratch directory are named by absolute paths");
    }
    std::error_code error;
    if (!std::filesystem::is_directory(setup_.scratch_directory, error)) {
        throw CompilerUnavailable("the scratch directory of the shader compiler does not exist");
    }
    identity_ = probed_version(setup_) + "; target " + std::string(target_environment);
}

Compilation GlslangProcessCompiler::compile(Stage stage, std::string_view source) {
    const ScratchDirectory scratch(setup_.scratch_directory);
    const bool is_vertex = stage == Stage::vertex;
    const std::filesystem::path input = scratch.path() / (is_vertex ? "shader.vert" : "shader.frag");
    const std::filesystem::path output = scratch.path() / "shader.spv";
    write_source(input, source);

    const std::array<std::string, 9> arguments{
        "-V", "--quiet", "--target-env", std::string(target_environment), "-S", is_vertex ? "vert" : "frag",
        "-o", output.string(), input.string(),
    };
    const ProcessResult result = run_process(setup_.executable, arguments, setup_.limits);
    if (result.end == ProcessEnd::timed_out) {
        return Compilation{
            {}, "the shader compiler did not finish within " + std::to_string(setup_.limits.wall_time.count()) + " ms"};
    }
    if (result.end == ProcessEnd::stopped_by_signal) {
        return Compilation{{}, "the shader compiler was stopped by signal " + std::to_string(result.exit_code)};
    }
    if (result.exit_code != 0) {
        return Compilation{{}, result.output};
    }
    std::vector<std::uint32_t> spirv = read_spirv(output, setup_.max_spirv_words);
    if (spirv.empty()) {
        return Compilation{
            {}, "the shader compiler wrote no SPIR-V within " + std::to_string(setup_.max_spirv_words) + " words"};
    }
    return Compilation{std::move(spirv), result.output};
}

}
