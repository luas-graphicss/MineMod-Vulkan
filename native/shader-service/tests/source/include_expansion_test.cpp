#include <string>

#include <doctest.h>

#include "source/include_expansion.hpp"
#include "source/source_error.hpp"

namespace {

using namespace minemod::shaders;

std::string failure_of(std::string_view text, const IncludeSources& includes, const SourceLimits& limits) {
    try {
        expand_includes("entity.vsh", text, includes, limits);
    } catch (const SourceError& error) {
        return error.what();
    }
    return "expanded";
}

std::string failure_of(std::string_view text, const IncludeSources& includes) {
    return failure_of(text, includes, default_source_limits);
}

}

TEST_CASE("an import is replaced by the file, between markers that keep the line numbers of both") {
    const IncludeSources includes{{"<fog.glsl>", "#version 150\nfloat fog() {\n    return 1.0;\n}\n"}};

    const ExpandedSource expanded =
        expand_includes("entity.vsh", "#version 150\n#moj_import <fog.glsl>\nvoid main() {}\n", includes);

    CHECK(
        expanded.text
        == "#version 150\n"
           "#line 1 1\n"
           "\n"
           "float fog() {\n"
           "    return 1.0;\n"
           "}\n"
           "#line 3 0\n"
           "void main() {}\n");
    CHECK(expanded.files == std::vector<std::string>{"entity.vsh", "<fog.glsl>"});
}

TEST_CASE("a file imported twice keeps one index and quoted names differ from bracketed ones") {
    const IncludeSources includes{{"<light.glsl>", "float bright;\n"}, {"\"light.glsl\"", "float dim;\n"}};

    const ExpandedSource expanded = expand_includes(
        "entity.vsh",
        "#version 150\n#moj_import <light.glsl>\n#moj_import \"light.glsl\"\n  #  moj_import <light.glsl> // again\n",
        includes);

    CHECK(expanded.files == std::vector<std::string>{"entity.vsh", "<light.glsl>", "\"light.glsl\""});
    CHECK(expanded.text.find("#line 1 2\nfloat dim;\n#line 4 0\n#line 1 1\nfloat bright;\n#line 5 0\n") != std::string::npos);
}

TEST_CASE("an import inside a block comment stays a comment and carriage returns are dropped") {
    const ExpandedSource expanded = expand_includes(
        "entity.vsh", "#version 150\r\n/* disabled\n#moj_import <missing.glsl>\n*/\r\nvoid main() {}\r\n", {});

    CHECK(expanded.text == "#version 150\n/* disabled\n#moj_import <missing.glsl>\n*/\nvoid main() {}\n");
}

TEST_CASE("a missing file and a malformed import are reported with the importing line") {
    CHECK(
        failure_of("#version 150\n\n#moj_import <fog.glsl>\n", {})
        == "entity.vsh:3: <fog.glsl> is not provided by the resource packs");
    CHECK(
        failure_of("#version 150\n#moj_import fog.glsl\n", {})
        == "entity.vsh:2: the import names no file between <> or quotes");
    CHECK(
        failure_of("#version 150\n#moj_import <fog.glsl> float x;\n", {{"<fog.glsl>", ""}})
        == "entity.vsh:2: the import names no file between <> or quotes");
}

TEST_CASE("a file that imports itself through another one is refused") {
    const IncludeSources includes{
        {"<a.glsl>", "#moj_import <b.glsl>\n"},
        {"<b.glsl>", "\n#moj_import <a.glsl>\n"},
    };

    CHECK(failure_of("#version 150\n#moj_import <a.glsl>\n", includes) == "<b.glsl>:2: <a.glsl> imports itself");
}

TEST_CASE("nesting, the number of imports and the sizes of the source are limited") {
    const IncludeSources chain{
        {"<a.glsl>", "#moj_import <b.glsl>\n"},
        {"<b.glsl>", "#moj_import <c.glsl>\n"},
        {"<c.glsl>", "float deep;\n"},
    };
    const SourceLimits two_levels{1024, 4096, 2, 64};
    const SourceLimits two_imports{1024, 4096, 8, 2};
    const SourceLimits small_files{16, 4096, 8, 64};
    const SourceLimits small_result{1024, 24, 8, 64};

    CHECK(
        failure_of("#version 150\n#moj_import <a.glsl>\n", chain, two_levels)
        == "<b.glsl>:1: imports are nested deeper than 2 files");
    CHECK(
        failure_of("#version 150\n#moj_import <a.glsl>\n", chain, two_imports)
        == "<b.glsl>:1: the shader imports more than 2 files");
    CHECK(
        failure_of("#version 150\nvoid main() {}\n", {}, small_files) == "entity.vsh is larger than 16 bytes");
    CHECK(
        failure_of("#version 150\nvoid main() {}\n", {}, small_result)
        == "entity.vsh expands to more than 24 bytes");
}

TEST_CASE("a source with a NUL byte is refused") {
    CHECK(failure_of(std::string_view("#version 150\n\0void", 18), {}) == "entity.vsh contains a NUL byte");
}
