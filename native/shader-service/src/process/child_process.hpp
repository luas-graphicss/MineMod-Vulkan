#pragma once

#include <chrono>
#include <cstddef>
#include <cstdint>
#include <filesystem>
#include <span>
#include <stdexcept>
#include <string>

namespace minemod::shaders {

class ProcessError : public std::runtime_error {
public:
    using std::runtime_error::runtime_error;
};

struct ProcessLimits {
    std::chrono::milliseconds wall_time;
    std::chrono::seconds cpu_time;
    std::uint64_t address_space_bytes;
    std::uint64_t written_file_bytes;
    std::size_t captured_output_bytes;
};

enum class ProcessEnd { exited, stopped_by_signal, timed_out };

struct ProcessResult {
    ProcessEnd end;
    int exit_code;
    std::string output;
};

ProcessResult run_process(
    const std::filesystem::path& executable, std::span<const std::string> arguments, const ProcessLimits& limits);

}
