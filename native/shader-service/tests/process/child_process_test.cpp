#include <array>
#include <chrono>
#include <cstdint>
#include <string>

#include <doctest.h>

#include "process/child_process.hpp"

namespace {

using namespace minemod::shaders;
using namespace std::chrono_literals;

constexpr std::uint64_t one_gibibyte = std::uint64_t{1} << 30;
constexpr std::uint64_t one_mebibyte = std::uint64_t{1} << 20;
const ProcessLimits generous{5000ms, 5s, one_gibibyte, one_mebibyte, 4096};

ProcessResult run_shell(const std::string& script, const ProcessLimits& limits) {
    const std::array<std::string, 2> arguments{"-c", script};
    return run_process("/bin/sh", arguments, limits);
}

}

TEST_CASE("the exit code and the merged output of the child are returned") {
    const ProcessResult result = run_shell("echo out; echo err >&2; exit 3", generous);

    CHECK(result.end == ProcessEnd::exited);
    CHECK(result.exit_code == 3);
    CHECK(result.output == "out\nerr\n");
}

TEST_CASE("the child starts without the environment of the game and without input") {
    const ProcessResult result = run_shell("echo \"[$HOME]\"; read line; echo \"read=$?\"", generous);

    CHECK(result.output == "[]\nread=1\n");
}

TEST_CASE("the captured output stops at its capacity") {
    ProcessLimits eight_bytes = generous;
    eight_bytes.captured_output_bytes = 8;

    const ProcessResult result = run_shell("echo 0123456789abcdef", eight_bytes);

    CHECK(result.end == ProcessEnd::exited);
    CHECK(result.output == "01234567");
}

TEST_CASE("a child that outlives its wall time is stopped") {
    ProcessLimits brief = generous;
    brief.wall_time = 200ms;
    const auto started = std::chrono::steady_clock::now();

    const ProcessResult result = run_shell("exec /bin/sleep 30", brief);

    CHECK(result.end == ProcessEnd::timed_out);
    CHECK(std::chrono::steady_clock::now() - started < 5s);
}

TEST_CASE("a child that spends its processor time is stopped by a signal") {
    ProcessLimits one_second = generous;
    one_second.wall_time = 20s;
    one_second.cpu_time = 1s;

    const ProcessResult result = run_shell("while :; do :; done", one_second);

    CHECK(result.end == ProcessEnd::stopped_by_signal);
}

TEST_CASE("a program that does not exist cannot be started") {
    CHECK_THROWS_AS(run_process("/nonexistent/minemod-compiler", {}, generous), ProcessError);
}
