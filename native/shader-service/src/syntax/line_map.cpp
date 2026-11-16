#include "syntax/line_map.hpp"

#include <charconv>
#include <optional>
#include <string_view>

namespace minemod::shaders {

namespace {

constexpr std::string_view line_directive = "line";

std::string_view without_leading_blanks(std::string_view text) noexcept {
    const std::size_t first = text.find_first_not_of(" \t");
    return first == std::string_view::npos ? std::string_view{} : text.substr(first);
}

std::optional<std::size_t> take_number(std::string_view& text) noexcept {
    text = without_leading_blanks(text);
    std::size_t number = 0;
    const auto parsed = std::from_chars(text.data(), text.data() + text.size(), number);
    if (parsed.ec != std::errc{}) {
        return std::nullopt;
    }
    text.remove_prefix(static_cast<std::size_t>(parsed.ptr - text.data()));
    return number;
}

std::optional<SourceOrigin> following_origin(std::string_view directive, std::size_t current_file) noexcept {
    std::string_view rest = without_leading_blanks(directive.substr(1));
    if (!rest.starts_with(line_directive)) {
        return std::nullopt;
    }
    rest.remove_prefix(line_directive.size());
    const std::optional<std::size_t> line = take_number(rest);
    if (!line) {
        return std::nullopt;
    }
    return SourceOrigin{take_number(rest).value_or(current_file), *line};
}

}

LineMap::LineMap(std::span<const Token> tokens) {
    std::size_t current_file = 0;
    for (const Token& token : tokens) {
        if (token.kind != TokenKind::directive) {
            continue;
        }
        if (const std::optional<SourceOrigin> following = following_origin(token.text, current_file)) {
            markers_.push_back(Marker{token.line, *following});
            current_file = following->file;
        }
    }
}

SourceOrigin LineMap::origin_of(std::size_t physical_line) const noexcept {
    for (auto marker = markers_.rbegin(); marker != markers_.rend(); ++marker) {
        if (marker->physical_line < physical_line) {
            const std::size_t lines_after_marker = physical_line - marker->physical_line - 1;
            return SourceOrigin{marker->following.file, marker->following.line + lines_after_marker};
        }
    }
    return SourceOrigin{0, physical_line + 1};
}

std::string describe(const SourceOrigin& origin, std::span<const std::string> files) {
    const std::string file = origin.file < files.size() ? files[origin.file] : "source " + std::to_string(origin.file);
    return file + ":" + std::to_string(origin.line);
}

}
