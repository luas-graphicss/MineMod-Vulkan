#include <string>
#include <vector>

#include <doctest.h>

#include "syntax/glsl_tokens.hpp"
#include "syntax/line_map.hpp"

namespace {

using namespace minemod::shaders;

}

TEST_CASE("lines before any marker belong to the first file") {
    const std::vector<Token> tokens = tokenize("#version 150\nvoid main() {}\n");
    const LineMap lines(tokens);

    const SourceOrigin origin = lines.origin_of(1);

    CHECK(origin.file == 0);
    CHECK(origin.line == 2);
}

TEST_CASE("a marker names the file and the number of the line after it") {
    const std::vector<Token> tokens = tokenize("#version 150\n#line 1 2\nfloat a;\nfloat b;\n#line 7 0\nfloat c;\n");
    const LineMap lines(tokens);
    const std::vector<std::string> files{"entity.vsh", "<fog.glsl>", "<light.glsl>"};

    CHECK(describe(lines.origin_of(3), files) == "<light.glsl>:2");
    CHECK(describe(lines.origin_of(5), files) == "entity.vsh:7");
}

TEST_CASE("a marker without a file keeps the current one and an unknown file is described by its number") {
    const std::vector<Token> tokens = tokenize("#line 1 5\nfloat a;\n#line 40\nfloat b;\n");
    const LineMap lines(tokens);
    const std::vector<std::string> files{"entity.vsh"};

    CHECK(describe(lines.origin_of(3), files) == "source 5:40");
}
