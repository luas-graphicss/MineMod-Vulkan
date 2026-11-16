#pragma once

#include <cstddef>
#include <string_view>
#include <vector>

namespace minemod::shaders {

enum class TokenKind { identifier, number, punctuation, directive };

struct Token {
    TokenKind kind;
    std::string_view text;
    std::size_t line;
};

std::vector<Token> tokenize(std::string_view source);

}
