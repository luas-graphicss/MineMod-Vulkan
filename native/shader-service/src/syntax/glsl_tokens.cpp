#include "syntax/glsl_tokens.hpp"

#include <algorithm>
#include <string>
#include <utility>

#include "source/source_error.hpp"

namespace minemod::shaders {

namespace {

constexpr std::string_view punctuation = "{}()[];,.=+-*/%<>!&|^~?:";

bool is_letter(char symbol) noexcept {
    return (symbol >= 'a' && symbol <= 'z') || (symbol >= 'A' && symbol <= 'Z') || symbol == '_';
}

bool is_digit(char symbol) noexcept {
    return symbol >= '0' && symbol <= '9';
}

bool is_blank(char symbol) noexcept {
    return symbol == ' ' || symbol == '\t' || symbol == '\r' || symbol == '\f' || symbol == '\v';
}

class Scanner {
public:
    explicit Scanner(std::string_view source) noexcept : source_(source) {}

    std::vector<Token> scan() {
        while (position_ < source_.size()) {
            scan_next();
        }
        return std::move(tokens_);
    }

private:
    void scan_next() {
        const char symbol = source_[position_];
        if (symbol == '\n') {
            start_line();
            ++position_;
        } else if (is_blank(symbol)) {
            ++position_;
        } else if (rest().starts_with("//")) {
            position_ = std::min(source_.find('\n', position_), source_.size());
        } else if (rest().starts_with("/*")) {
            skip_block_comment();
        } else if (symbol == '#' && !line_has_token_) {
            add_directive();
        } else if (is_letter(symbol)) {
            add(TokenKind::identifier, word_length());
        } else if (is_digit(symbol) || (symbol == '.' && is_followed_by_digit())) {
            add(TokenKind::number, number_length());
        } else if (punctuation.find(symbol) != std::string_view::npos) {
            add(TokenKind::punctuation, 1);
        } else {
            throw SyntaxError(
                line_, "the shader contains the unexpected byte " + std::to_string(static_cast<unsigned char>(symbol)));
        }
    }

    std::string_view rest() const noexcept {
        return source_.substr(position_);
    }

    void start_line() noexcept {
        ++line_;
        line_has_token_ = false;
    }

    void skip_block_comment() {
        const std::size_t end = source_.find("*/", position_ + 2);
        if (end == std::string_view::npos) {
            throw SyntaxError(line_, "a block comment is never closed");
        }
        const std::string_view comment = source_.substr(position_, end - position_);
        const auto crossed_lines = static_cast<std::size_t>(std::ranges::count(comment, '\n'));
        if (crossed_lines != 0) {
            line_ += crossed_lines;
            line_has_token_ = false;
        }
        position_ = end + 2;
    }

    void add_directive() {
        const std::size_t first_line = line_;
        std::size_t end = position_;
        while (true) {
            end = std::min(source_.find('\n', end), source_.size());
            const bool continues = end < source_.size() && source_[end - 1] == '\\';
            if (!continues) {
                break;
            }
            ++line_;
            ++end;
        }
        tokens_.push_back(Token{TokenKind::directive, source_.substr(position_, end - position_), first_line});
        position_ = end;
    }

    std::size_t word_length() const noexcept {
        std::size_t end = position_ + 1;
        while (end < source_.size() && (is_letter(source_[end]) || is_digit(source_[end]))) {
            ++end;
        }
        return end - position_;
    }

    bool is_followed_by_digit() const noexcept {
        return position_ + 1 < source_.size() && is_digit(source_[position_ + 1]);
    }

    std::size_t number_length() const noexcept {
        std::size_t end = position_ + 1;
        while (end < source_.size() && continues_number(end)) {
            ++end;
        }
        return end - position_;
    }

    bool continues_number(std::size_t index) const noexcept {
        const char symbol = source_[index];
        const char previous = source_[index - 1];
        const bool is_exponent_sign = (symbol == '+' || symbol == '-') && (previous == 'e' || previous == 'E');
        return is_letter(symbol) || is_digit(symbol) || symbol == '.' || is_exponent_sign;
    }

    void add(TokenKind kind, std::size_t length) {
        tokens_.push_back(Token{kind, source_.substr(position_, length), line_});
        position_ += length;
        line_has_token_ = true;
    }

    std::string_view source_;
    std::size_t position_ = 0;
    std::size_t line_ = 0;
    bool line_has_token_ = false;
    std::vector<Token> tokens_;
};

}

std::vector<Token> tokenize(std::string_view source) {
    return Scanner(source).scan();
}

}
