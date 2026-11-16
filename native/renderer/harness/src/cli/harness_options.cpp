#include "cli/harness_options.hpp"

#include <charconv>
#include <cstdint>
#include <iterator>
#include <string>
#include <system_error>

namespace minemod::harness {

namespace {

constexpr std::uint32_t max_presented_frames = 10000;

std::uint32_t parse_frame_count(std::string_view text) {
    std::uint32_t frames = 0;
    const char* const end = text.data() + text.size();
    const auto [parsed_end, error] = std::from_chars(text.data(), end, frames);
    if (error != std::errc{} || parsed_end != end || frames > max_presented_frames) {
        throw UsageError(
            "--frames takes a count from 0 to " + std::to_string(max_presented_frames) + ", not '" + std::string(text)
            + "'");
    }
    return frames;
}

}

HarnessOptions parse_harness_options(std::span<const std::string_view> arguments) {
    HarnessOptions options;
    for (auto argument = arguments.begin(); argument != arguments.end(); ++argument) {
        if (*argument == "--validation") {
            options.validation = renderer::Validation::required;
        } else if (*argument == "--frames" && std::next(argument) != arguments.end()) {
            options.presented_frames = parse_frame_count(*++argument);
        } else {
            throw UsageError("unknown or incomplete argument '" + std::string(*argument) + "'");
        }
    }
    return options;
}

}
