#include <string>
#include <vector>

#include <doctest.h>

#include "source/source_error.hpp"
#include "syntax/glsl_tokens.hpp"

namespace {

using namespace minemod::shaders;

std::vector<std::string> texts_of(const std::vector<Token>& tokens) {
    std::vector<std::string> texts;
    for (const Token& token : tokens) {
        texts.emplace_back(token.text);
    }
    return texts;
}

}

TEST_CASE("words, numbers and punctuation become tokens and comments disappear") {
    const std::vector<Token> tokens = tokenize("float x = 1.5e-3; // gone\nx /* gone\n too */ += .5;");

    CHECK(texts_of(tokens) == std::vector<std::string>{"float", "x", "=", "1.5e-3", ";", "x", "+", "=", ".5", ";"});
    CHECK(tokens[3].kind == TokenKind::number);
    CHECK(tokens[5].line == 1);
    CHECK(tokens[6].line == 2);
}

TEST_CASE("a directive is one token up to the end of its continued line") {
    const std::vector<Token> tokens = tokenize("  #define A \\\n  1\nint a;");

    REQUIRE(tokens.size() == 4);
    CHECK(tokens[0].kind == TokenKind::directive);
    CHECK(tokens[0].text == "#define A \\\n  1");
    CHECK(tokens[0].line == 0);
    CHECK(tokens[1].line == 2);
    CHECK(tokens[3].kind == TokenKind::punctuation);
}

TEST_CASE("a comment that never closes, a directive after code and a byte outside GLSL are refused") {
    CHECK_THROWS_WITH_AS(tokenize("int a;\n/* open"), "a block comment is never closed", SyntaxError);
    CHECK_THROWS_AS(tokenize("int a; #define B"), SyntaxError);
    try {
        tokenize("int a;\nint b = `;");
        FAIL("the backtick was accepted");
    } catch (const SyntaxError& error) {
        CHECK(error.line() == 1);
        CHECK(std::string(error.what()) == "the shader contains the unexpected byte 96");
    }
}
