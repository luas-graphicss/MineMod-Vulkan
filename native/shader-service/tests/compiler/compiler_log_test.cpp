#include <string>
#include <vector>

#include <doctest.h>

#include "compiler/compiler_log.hpp"

namespace {

using namespace minemod::shaders;

const std::vector<std::string> files{"entity.fsh", "<fog.glsl>"};

}

TEST_CASE("compiler errors name the pack file instead of its number") {
    const std::string log =
        "ERROR: 0:12: 'glow' : undeclared identifier \n"
        "ERROR: 1:3: '' : compilation terminated \n"
        "ERROR: 2 compilation errors.  No code generated.\n"
        "\n"
        "\n"
        "SPIR-V is not generated for failed compile or link\n";

    CHECK(
        located_errors(log, files)
        == std::vector<std::string>{
            "entity.fsh:12: 'glow' : undeclared identifier",
            "<fog.glsl>:3: '' : compilation terminated",
            "2 compilation errors.  No code generated.",
        });
}

TEST_CASE("an error about a file the shader does not have keeps its number") {
    CHECK(located_errors("ERROR: 7:1: 'x' : syntax error\n", files) == std::vector<std::string>{"7:1: 'x' : syntax error"});
}

TEST_CASE("a log without errors is returned line by line") {
    CHECK(
        located_errors("the shader compiler did not finish within 100 ms\n", files)
        == std::vector<std::string>{"the shader compiler did not finish within 100 ms"});
    CHECK(located_errors("", files).empty());
}
