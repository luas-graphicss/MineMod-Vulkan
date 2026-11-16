#include "compiler/compiler_log.hpp"

#include <algorithm>
#include <charconv>
#include <cstddef>

namespace minemod::shaders {

namespace {

constexpr std::string_view error_prefix = "ERROR: ";

std::vector<std::string_view> filled_lines(std::string_view log) {
    std::vector<std::string_view> lines;
    std::size_t start = 0;
    while (start < log.size()) {
        const std::size_t end = std::min(log.find('\n', start), log.size());
        const std::string_view line = log.substr(start, end - start);
        const std::size_t last_filled = line.find_last_not_of(" \t\r");
        if (last_filled != std::string_view::npos) {
            lines.push_back(line.substr(0, last_filled + 1));
        }
        start = end + 1;
    }
    return lines;
}

std::string with_file_name(std::string_view error, std::span<const std::string> files) {
    const std::size_t separator = error.find(':');
    if (separator == std::string_view::npos) {
        return std::string(error);
    }
    std::size_t file = 0;
    const auto parsed = std::from_chars(error.data(), error.data() + separator, file);
    const bool names_a_file = parsed.ec == std::errc{} && parsed.ptr == error.data() + separator && file < files.size();
    return names_a_file ? files[file] + std::string(error.substr(separator)) : std::string(error);
}

}

std::vector<std::string> located_errors(std::string_view log, std::span<const std::string> files) {
    const std::vector<std::string_view> lines = filled_lines(log);
    std::vector<std::string> errors;
    for (const std::string_view line : lines) {
        if (line.starts_with(error_prefix)) {
            errors.push_back(with_file_name(line.substr(error_prefix.size()), files));
        }
    }
    if (errors.empty()) {
        errors.assign(lines.begin(), lines.end());
    }
    return errors;
}

}
