#include <string>
#include <vector>

#include <doctest.h>

#include "source/source_error.hpp"
#include "syntax/glsl_tokens.hpp"
#include "syntax/interface_declarations.hpp"

namespace {

using namespace minemod::shaders;

std::vector<InterfaceDeclaration> declarations_of(const std::vector<Token>& tokens) {
    return parse_interface(tokens);
}

}

TEST_CASE("inputs, outputs and uniforms at the top level are found with their type and name") {
    const std::vector<Token> tokens = tokenize(
        "#version 150\n"
        "in vec3 Position;\n"
        "uniform mat4 ProjMat;\n"
        "flat out int parity;\n"
        "out highp vec2 coordinate;\n");

    const std::vector<InterfaceDeclaration> declarations = declarations_of(tokens);

    REQUIRE(declarations.size() == 4);
    CHECK(declarations[0].kind == InterfaceKind::input);
    CHECK(declarations[0].type == "vec3");
    CHECK(declarations[0].names == std::vector<std::string_view>{"Position"});
    CHECK(declarations[1].kind == InterfaceKind::uniform);
    CHECK(declarations[2].kind == InterfaceKind::output);
    CHECK(declarations[2].interpolation == "flat");
    CHECK(declarations[3].type == "vec2");
    CHECK(tokens[declarations[3].last_token].text == ";");
}

TEST_CASE("function parameters, struct members and locals are not interface declarations") {
    const std::vector<Token> tokens = tokenize(
        "struct Light { vec3 direction; };\n"
        "void shade(in vec3 normal, out vec3 color) { color = normal; }\n"
        "const float strength = 0.6;\n"
        "void main() { vec3 result; shade(vec3(0.0), result); }\n");

    CHECK(declarations_of(tokens).empty());
}

TEST_CASE("layouts, blocks, arrays, initializers and shared statements are marked") {
    const std::vector<Token> tokens = tokenize(
        "layout(location = 3) in vec3 Position;\n"
        "uniform Globals { mat4 view; } globals;\n"
        "uniform float weights[4];\n"
        "uniform float scale = 2.0;\n"
        "out vec2 first, second;\n"
        "layout(early_fragment_tests) in;\n");

    const std::vector<InterfaceDeclaration> declarations = declarations_of(tokens);

    REQUIRE(declarations.size() == 6);
    CHECK(declarations[0].has_layout);
    CHECK(declarations[1].is_block);
    CHECK(declarations[1].names == std::vector<std::string_view>{"globals"});
    CHECK(declarations[2].is_array);
    CHECK(declarations[3].has_initializer);
    CHECK(declarations[4].names.size() == 2);
    CHECK(declarations[5].has_layout);
    CHECK(declarations[5].names.empty());
}

TEST_CASE("a declaration that is cut short is a syntax error on its line") {
    try {
        declarations_of(tokenize("#version 150\n\nuniform mat4"));
        FAIL("the unfinished declaration was accepted");
    } catch (const SyntaxError& error) {
        CHECK(error.line() == 2);
        CHECK(std::string(error.what()) == "an interface declaration is missing a name");
    }
    CHECK_THROWS_AS(declarations_of(tokenize("void main() { } }")), SyntaxError);
}
