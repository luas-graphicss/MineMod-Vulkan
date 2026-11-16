#include "source/include_expansion.hpp"

#include <algorithm>
#include <optional>
#include <utility>

#include "source/source_error.hpp"

namespace minemod::shaders {

namespace {

constexpr std::string_view import_directive = "moj_import";
constexpr std::string_view version_directive = "version";
constexpr std::size_t max_import_key_bytes = 256;

struct Directive {
    std::string_view name;
    std::string_view argument;
};

bool is_blank(char symbol) noexcept {
    return symbol == ' ' || symbol == '\t';
}

bool is_name_letter(char symbol) noexcept {
    return (symbol >= 'a' && symbol <= 'z') || (symbol >= 'A' && symbol <= 'Z') || symbol == '_';
}

std::string_view without_leading_blanks(std::string_view text) noexcept {
    const auto first = std::ranges::find_if_not(text, is_blank);
    return text.substr(static_cast<std::size_t>(first - text.begin()));
}

std::optional<Directive> directive_in(std::string_view line) noexcept {
    std::string_view rest = without_leading_blanks(line);
    if (rest.empty() || rest.front() != '#') {
        return std::nullopt;
    }
    rest = without_leading_blanks(rest.substr(1));
    const auto name_end = std::ranges::find_if_not(rest, is_name_letter);
    const auto name_length = static_cast<std::size_t>(name_end - rest.begin());
    return Directive{rest.substr(0, name_length), without_leading_blanks(rest.substr(name_length))};
}

std::optional<std::string_view> import_key(std::string_view argument) noexcept {
    if (argument.empty() || (argument.front() != '<' && argument.front() != '"')) {
        return std::nullopt;
    }
    const char closing = argument.front() == '<' ? '>' : '"';
    const std::size_t end = argument.find(closing, 1);
    if (end == std::string_view::npos || end == 1 || end >= max_import_key_bytes) {
        return std::nullopt;
    }
    const std::string_view after = without_leading_blanks(argument.substr(end + 1));
    if (!after.empty() && !after.starts_with("//")) {
        return std::nullopt;
    }
    return argument.substr(0, end + 1);
}

bool ends_inside_block_comment(std::string_view line, bool starts_inside) noexcept {
    bool is_inside = starts_inside;
    for (std::size_t index = 0; index < line.size(); ++index) {
        const std::string_view rest = line.substr(index);
        if (is_inside && rest.starts_with("*/")) {
            is_inside = false;
            ++index;
        } else if (!is_inside && rest.starts_with("//")) {
            return false;
        } else if (!is_inside && rest.starts_with("/*")) {
            is_inside = true;
            ++index;
        }
    }
    return is_inside;
}

class Expansion {
public:
    Expansion(const IncludeSources& includes, const SourceLimits& limits) noexcept
        : includes_(includes), limits_(limits) {}

    void expand(std::string_view name, std::string_view text) {
        files_.emplace_back(name);
        expand_file(0, text);
    }

    ExpandedSource release() {
        return ExpandedSource{std::move(text_), std::move(files_)};
    }

private:
    void expand_file(std::size_t file, std::string_view text) {
        require_acceptable(file, text);
        bool is_inside_comment = false;
        std::size_t line_number = 1;
        std::size_t start = 0;
        while (start < text.size()) {
            const std::size_t end = std::min(text.find('\n', start), text.size());
            std::string_view line = text.substr(start, end - start);
            if (line.ends_with('\r')) {
                line.remove_suffix(1);
            }
            expand_line(file, line_number, line, is_inside_comment);
            is_inside_comment = ends_inside_block_comment(line, is_inside_comment);
            start = end + 1;
            ++line_number;
        }
    }

    void require_acceptable(std::size_t file, std::string_view text) const {
        if (text.size() > limits_.max_file_bytes) {
            throw SourceError(files_[file] + " is larger than " + std::to_string(limits_.max_file_bytes) + " bytes");
        }
        if (text.find('\0') != std::string_view::npos) {
            throw SourceError(files_[file] + " contains a NUL byte");
        }
    }

    void expand_line(std::size_t file, std::size_t line_number, std::string_view line, bool is_inside_comment) {
        const std::optional<Directive> directive = is_inside_comment ? std::nullopt : directive_in(line);
        if (directive && directive->name == import_directive) {
            import(file, line_number, directive->argument);
            return;
        }
        const bool is_imported_version = directive && directive->name == version_directive && !imports_.empty();
        append(is_imported_version ? std::string_view{} : line);
    }

    void import(std::size_t importer, std::size_t line_number, std::string_view argument) {
        const std::string where = files_[importer] + ":" + std::to_string(line_number) + ": ";
        const std::optional<std::string_view> key = import_key(argument);
        if (!key) {
            throw SourceError(where + "the import names no file between <> or quotes");
        }
        const auto imported = includes_.find(*key);
        if (imported == includes_.end()) {
            throw SourceError(where + std::string(*key) + " is not provided by the resource packs");
        }
        if (std::ranges::find(imports_, *key) != imports_.end()) {
            throw SourceError(where + std::string(*key) + " imports itself");
        }
        if (imports_.size() == limits_.max_include_depth) {
            throw SourceError(
                where + "imports are nested deeper than " + std::to_string(limits_.max_include_depth) + " files");
        }
        if (import_count_ == limits_.max_includes) {
            throw SourceError(where + "the shader imports more than " + std::to_string(limits_.max_includes) + " files");
        }
        ++import_count_;

        const std::size_t file = file_index(imported->first);
        imports_.push_back(imported->first);
        append_line_marker(1, file);
        expand_file(file, imported->second);
        imports_.pop_back();
        append_line_marker(line_number + 1, importer);
    }

    std::size_t file_index(std::string_view key) {
        const auto known = std::find(files_.begin() + 1, files_.end(), key);
        if (known != files_.end()) {
            return static_cast<std::size_t>(known - files_.begin());
        }
        files_.emplace_back(key);
        return files_.size() - 1;
    }

    void append_line_marker(std::size_t next_line, std::size_t file) {
        append("#line " + std::to_string(next_line) + " " + std::to_string(file));
    }

    void append(std::string_view line) {
        if (line.size() + 1 > limits_.max_expanded_bytes - text_.size()) {
            throw SourceError(
                files_.front() + " expands to more than " + std::to_string(limits_.max_expanded_bytes) + " bytes");
        }
        text_.append(line);
        text_.push_back('\n');
    }

    const IncludeSources& includes_;
    SourceLimits limits_;
    std::string text_;
    std::vector<std::string> files_;
    std::vector<std::string_view> imports_;
    std::size_t import_count_ = 0;
};

}

ExpandedSource expand_includes(
    std::string_view name, std::string_view text, const IncludeSources& includes, const SourceLimits& limits) {
    Expansion expansion(includes, limits);
    expansion.expand(name, text);
    return expansion.release();
}

}
