#include "minemod/shaders/shader_service.h"

#include <algorithm>
#include <chrono>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <filesystem>
#include <memory>
#include <new>
#include <optional>
#include <span>
#include <string>
#include <string_view>
#include <utility>

#include "compiler/glslang_process_compiler.hpp"
#include "minemod/abi/negotiation.h"
#include "service/shader_service.hpp"

static_assert(sizeof(minemod_shader_service_config) == 32 + 2 * sizeof(void*));
static_assert(sizeof(minemod_shader_text) == sizeof(void*) + 8);
static_assert(sizeof(minemod_core_shader_source) == 8 + 3 * sizeof(minemod_shader_text) + sizeof(void*));

struct minemod_shader_service {
    minemod_shader_service(
        std::unique_ptr<minemod::shaders::ShaderCompiler> compiler, const minemod::shaders::ServiceLimits& limits)
        : core(std::move(compiler), limits) {}

    minemod::shaders::ShaderService core;
};

struct minemod_shader_compilation {
    minemod::shaders::ProgramCompilation core;
    std::string text;
};

namespace {

using minemod::shaders::CompilerSetup;
using minemod::shaders::CompilerUnavailable;
using minemod::shaders::GlslangProcessCompiler;
using minemod::shaders::IncludeSources;
using minemod::shaders::ProcessLimits;
using minemod::shaders::ServiceLimits;

constexpr std::uint64_t written_file_bytes = std::uint64_t{1} << 23;
constexpr std::size_t captured_output_bytes = std::size_t{1} << 16;
constexpr std::size_t max_spirv_words = std::size_t{1} << 20;

template <typename Operation>
minemod_result run_guarded(Operation&& operation) noexcept {
    try {
        return operation();
    } catch (const std::bad_alloc&) {
        return MINEMOD_RESULT_OUT_OF_MEMORY;
    } catch (const CompilerUnavailable&) {
        return MINEMOD_RESULT_UNSUPPORTED_CAPABILITY;
    } catch (...) {
        return MINEMOD_RESULT_INTERNAL_ERROR;
    }
}

minemod_result check_abi_version(minemod_abi_version version) noexcept {
    const minemod_negotiation_request request{
        .struct_bytes = sizeof(minemod_negotiation_request),
        .client_version = version,
        .required_capabilities = 0,
    };
    minemod_negotiation negotiation{};
    return minemod_negotiate(&request, &negotiation);
}

bool is_within(std::uint64_t value, std::uint64_t smallest, std::uint64_t largest) noexcept {
    return value >= smallest && value <= largest;
}

std::optional<std::filesystem::path> absolute_path(const char* text) {
    if (text == nullptr) {
        return std::nullopt;
    }
    const std::size_t length = ::strnlen(text, MINEMOD_SHADER_MAX_PATH_BYTES);
    if (length == 0 || length == MINEMOD_SHADER_MAX_PATH_BYTES) {
        return std::nullopt;
    }
    std::filesystem::path path(std::string_view(text, length));
    return path.is_absolute() ? std::optional(std::move(path)) : std::nullopt;
}

minemod_result check_config(const minemod_shader_service_config& config) noexcept {
    if (config.struct_bytes != sizeof(minemod_shader_service_config) || config.reserved != 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const bool fits_limits = is_within(
                                 config.wall_time_milliseconds,
                                 MINEMOD_SHADER_MIN_WALL_TIME_MILLISECONDS,
                                 MINEMOD_SHADER_MAX_WALL_TIME_MILLISECONDS)
        && is_within(
            config.compiler_memory_bytes,
            MINEMOD_SHADER_MIN_COMPILER_MEMORY_BYTES,
            MINEMOD_SHADER_MAX_COMPILER_MEMORY_BYTES)
        && config.cache_bytes <= MINEMOD_SHADER_MAX_CACHE_BYTES;
    if (!fits_limits) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    return check_abi_version(config.abi_version);
}

ProcessLimits process_limits(const minemod_shader_service_config& config) noexcept {
    const std::chrono::milliseconds wall_time(config.wall_time_milliseconds);
    return ProcessLimits{
        wall_time,
        std::chrono::ceil<std::chrono::seconds>(wall_time),
        config.compiler_memory_bytes,
        written_file_bytes,
        captured_output_bytes,
    };
}

bool is_printable(std::string_view text) noexcept {
    return std::ranges::all_of(text, [](char symbol) { return symbol >= ' ' && symbol <= '~'; });
}

std::optional<std::string_view> view_of(const minemod_shader_text& text, std::uint64_t max_bytes) noexcept {
    if (text.length > max_bytes || (text.bytes == nullptr && text.length != 0)) {
        return std::nullopt;
    }
    return std::string_view(text.bytes, static_cast<std::size_t>(text.length));
}

std::optional<std::string_view> name_of(const minemod_shader_text& text) noexcept {
    const std::optional<std::string_view> name = view_of(text, MINEMOD_SHADER_MAX_NAME_BYTES);
    return name && !name->empty() && is_printable(*name) ? name : std::nullopt;
}

minemod_result collect_includes(const minemod_core_shader_source& source, IncludeSources& includes) {
    if (source.include_count > MINEMOD_SHADER_MAX_INCLUDES) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    if (source.include_count != 0 && source.includes == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    for (const minemod_shader_include& include : std::span(source.includes, source.include_count)) {
        const std::optional<std::string_view> key = name_of(include.key);
        const std::optional<std::string_view> text = view_of(include.text, MINEMOD_SHADER_MAX_SOURCE_BYTES);
        if (!key || !text) {
            return MINEMOD_RESULT_INVALID_ARGUMENT;
        }
        includes.insert_or_assign(std::string(*key), std::string(*text));
    }
    return MINEMOD_RESULT_OK;
}

std::string joined(std::span<const std::string> lines) {
    std::string text;
    for (const std::string& line : lines) {
        text += line;
        text += '\n';
    }
    return text;
}

}

minemod_result minemod_shader_service_create(
    const minemod_shader_service_config* config, minemod_shader_service** service) noexcept {
    if (config == nullptr || service == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    *service = nullptr;
    if (const minemod_result checked = check_config(*config); checked != MINEMOD_RESULT_OK) {
        return checked;
    }
    return run_guarded([&]() -> minemod_result {
        std::optional<std::filesystem::path> compiler = absolute_path(config->compiler_path);
        std::optional<std::filesystem::path> scratch = absolute_path(config->scratch_directory);
        if (!compiler || !scratch) {
            return MINEMOD_RESULT_INVALID_ARGUMENT;
        }
        auto process_compiler = std::make_unique<GlslangProcessCompiler>(
            CompilerSetup{std::move(*compiler), std::move(*scratch), process_limits(*config), max_spirv_words});
        const ServiceLimits limits{
            minemod::shaders::default_source_limits, static_cast<std::size_t>(config->cache_bytes)};
        *service = std::make_unique<minemod_shader_service>(std::move(process_compiler), limits).release();
        return MINEMOD_RESULT_OK;
    });
}

void minemod_shader_service_destroy(minemod_shader_service* service) noexcept {
    std::unique_ptr<minemod_shader_service> owned(service);
}

minemod_result minemod_shader_service_compile_core_shader(
    minemod_shader_service* service,
    const minemod_core_shader_source* source,
    minemod_shader_compilation** compilation) noexcept {
    if (service == nullptr || source == nullptr || compilation == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    *compilation = nullptr;
    if (source->struct_bytes != sizeof(minemod_core_shader_source)) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return run_guarded([&]() -> minemod_result {
        const std::optional<std::string_view> name = name_of(source->name);
        const std::optional<std::string_view> vertex = view_of(source->vertex, MINEMOD_SHADER_MAX_SOURCE_BYTES);
        const std::optional<std::string_view> fragment = view_of(source->fragment, MINEMOD_SHADER_MAX_SOURCE_BYTES);
        if (!name || !vertex || !fragment) {
            return MINEMOD_RESULT_INVALID_ARGUMENT;
        }
        IncludeSources includes;
        if (const minemod_result collected = collect_includes(*source, includes); collected != MINEMOD_RESULT_OK) {
            return collected;
        }
        auto compiled = std::make_unique<minemod_shader_compilation>();
        compiled->core = service->core.compile_core_shader({*name, *vertex, *fragment}, includes);
        compiled->text = joined(compiled->core.problems);
        *compilation = compiled.release();
        return MINEMOD_RESULT_OK;
    });
}

void minemod_shader_compilation_destroy(minemod_shader_compilation* compilation) noexcept {
    std::unique_ptr<minemod_shader_compilation> owned(compilation);
}

uint32_t minemod_shader_compilation_succeeded(const minemod_shader_compilation* compilation) noexcept {
    return compilation != nullptr && compilation->core.program.has_value() ? 1 : 0;
}

uint64_t minemod_shader_compilation_read_words(
    const minemod_shader_compilation* compilation, uint32_t stage, uint32_t* destination, uint64_t capacity) noexcept {
    const bool names_a_stage = stage == MINEMOD_SHADER_STAGE_VERTEX || stage == MINEMOD_SHADER_STAGE_FRAGMENT;
    if (compilation == nullptr || !compilation->core.program || !names_a_stage
        || (destination == nullptr && capacity != 0)) {
        return 0;
    }
    const std::vector<std::uint32_t>& words = stage == MINEMOD_SHADER_STAGE_VERTEX
        ? compilation->core.program->vertex
        : compilation->core.program->fragment;
    const auto copied = static_cast<std::size_t>(std::min<std::uint64_t>(capacity, words.size()));
    if (copied != 0) {
        std::memcpy(destination, words.data(), copied * sizeof(std::uint32_t));
    }
    return words.size();
}

uint64_t minemod_shader_compilation_read_text(
    const minemod_shader_compilation* compilation, char* destination, uint64_t capacity) noexcept {
    if (compilation == nullptr || (destination == nullptr && capacity != 0)) {
        return 0;
    }
    const std::string& text = compilation->text;
    const auto copied = static_cast<std::size_t>(std::min<std::uint64_t>(capacity, text.size()));
    if (copied != 0) {
        std::memcpy(destination, text.data(), copied);
    }
    return text.size();
}
