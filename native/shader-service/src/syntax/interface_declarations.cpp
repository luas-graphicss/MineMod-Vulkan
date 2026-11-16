#include "syntax/interface_declarations.hpp"

#include <algorithm>
#include <array>
#include <optional>
#include <string>
#include <utility>

#include "source/source_error.hpp"

namespace minemod::shaders {

namespace {

constexpr std::array<std::string_view, 3> interpolation_qualifiers{"flat", "smooth", "noperspective"};
constexpr std::array<std::string_view, 8> ignored_qualifiers{
    "centroid", "sample", "invariant", "precise", "highp", "mediump", "lowp", "const",
};

template <std::size_t Count>
bool contains(const std::array<std::string_view, Count>& words, std::string_view word) noexcept {
    return std::ranges::find(words, word) != words.end();
}

std::optional<InterfaceKind> interface_kind(std::string_view word) noexcept {
    if (word == "in") {
        return InterfaceKind::input;
    }
    if (word == "out") {
        return InterfaceKind::output;
    }
    if (word == "uniform" || word == "buffer") {
        return InterfaceKind::uniform;
    }
    return std::nullopt;
}

class Parser {
public:
    explicit Parser(std::span<const Token> tokens) noexcept : tokens_(tokens) {}

    std::vector<InterfaceDeclaration> parse() {
        std::size_t depth = 0;
        bool starts_statement = true;
        while (next_ < tokens_.size()) {
            const Token& token = tokens_[next_];
            if (token.kind == TokenKind::directive) {
                ++next_;
                continue;
            }
            if (depth == 0 && starts_statement && parse_declaration()) {
                continue;
            }
            if (token.text == "{") {
                ++depth;
            } else if (token.text == "}") {
                if (depth == 0) {
                    throw SyntaxError(token.line, "a closing brace has no opening brace");
                }
                --depth;
            }
            starts_statement = depth == 0 && (token.text == ";" || token.text == "}");
            ++next_;
        }
        return std::move(declarations_);
    }

private:
    bool parse_declaration() {
        InterfaceDeclaration declaration{};
        declaration.first_token = next_;
        std::size_t at = next_;
        if (text_at(at) == "layout") {
            declaration.has_layout = true;
            at = after_group(at + 1, "(", ")");
        }
        at = after_qualifiers(at, declaration);
        const std::optional<InterfaceKind> kind = interface_kind(text_at(at));
        if (!kind) {
            return false;
        }
        declaration.kind = *kind;
        at = after_qualifiers(at + 1, declaration);
        if (text_at(at) != ";") {
            declaration.type = word_at(at, "its type");
            at = text_at(at + 1) == "{" ? after_block(at + 1, declaration) : after_declarators(at + 1, declaration);
        }
        if (text_at(at) != ";") {
            throw SyntaxError(line_at(at), "an interface declaration does not end with a semicolon");
        }
        declaration.last_token = at;
        next_ = at + 1;
        declarations_.push_back(std::move(declaration));
        return true;
    }

    std::size_t after_qualifiers(std::size_t at, InterfaceDeclaration& declaration) const noexcept {
        while (true) {
            const std::string_view word = text_at(at);
            if (contains(interpolation_qualifiers, word)) {
                declaration.interpolation = word;
            } else if (!contains(ignored_qualifiers, word)) {
                return at;
            }
            ++at;
        }
    }

    std::size_t after_block(std::size_t opening, InterfaceDeclaration& declaration) const {
        declaration.is_block = true;
        std::size_t at = after_group(opening, "{", "}");
        if (at < tokens_.size() && tokens_[at].kind == TokenKind::identifier) {
            declaration.names.push_back(tokens_[at].text);
            ++at;
        }
        return after_array_size(at, declaration);
    }

    std::size_t after_declarators(std::size_t at, InterfaceDeclaration& declaration) const {
        at = after_array_size(at, declaration);
        while (true) {
            declaration.names.push_back(word_at(at, "a name"));
            at = after_array_size(at + 1, declaration);
            if (text_at(at) == "=") {
                declaration.has_initializer = true;
                at = initializer_end(at + 1);
            }
            if (text_at(at) != ",") {
                return at;
            }
            ++at;
        }
    }

    std::size_t after_array_size(std::size_t at, InterfaceDeclaration& declaration) const {
        if (text_at(at) != "[") {
            return at;
        }
        declaration.is_array = true;
        return after_group(at, "[", "]");
    }

    std::size_t initializer_end(std::size_t at) const noexcept {
        std::size_t depth = 0;
        for (; at < tokens_.size(); ++at) {
            const std::string_view text = tokens_[at].text;
            if (depth == 0 && (text == "," || text == ";")) {
                break;
            }
            if (text == "(" || text == "[" || text == "{") {
                ++depth;
            } else if ((text == ")" || text == "]" || text == "}") && depth != 0) {
                --depth;
            }
        }
        return at;
    }

    std::size_t after_group(std::size_t opening_at, std::string_view opening, std::string_view closing) const {
        if (text_at(opening_at) != opening) {
            throw SyntaxError(
                line_at(opening_at), "an interface declaration is missing '" + std::string(opening) + "'");
        }
        std::size_t depth = 0;
        for (std::size_t at = opening_at; at < tokens_.size(); ++at) {
            if (tokens_[at].text == opening) {
                ++depth;
            } else if (tokens_[at].text == closing && --depth == 0) {
                return at + 1;
            }
        }
        throw SyntaxError(line_at(opening_at), "'" + std::string(opening) + "' is never closed");
    }

    std::string_view text_at(std::size_t at) const noexcept {
        return at < tokens_.size() ? tokens_[at].text : std::string_view{};
    }

    std::string_view word_at(std::size_t at, std::string_view missing) const {
        if (at >= tokens_.size() || tokens_[at].kind != TokenKind::identifier) {
            throw SyntaxError(line_at(at), "an interface declaration is missing " + std::string(missing));
        }
        return tokens_[at].text;
    }

    std::size_t line_at(std::size_t at) const noexcept {
        return tokens_[std::min(at, tokens_.size() - 1)].line;
    }

    std::span<const Token> tokens_;
    std::size_t next_ = 0;
    std::vector<InterfaceDeclaration> declarations_;
};

}

std::vector<InterfaceDeclaration> parse_interface(std::span<const Token> tokens) {
    return Parser(tokens).parse();
}

}
