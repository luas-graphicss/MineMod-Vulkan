#include <cstdint>
#include <string>
#include <vector>

#include <doctest.h>

#include "adaptation/core_shader_bindings.hpp"
#include "cache/shader_cache.hpp"

namespace {

using namespace minemod::shaders;

const std::vector<std::uint32_t> four_words{1, 2, 3, 4};
constexpr std::size_t two_modules = 2 * (1 + 4 * sizeof(std::uint32_t));

}

TEST_CASE("a stored module is found again and an unknown key is not") {
    ShaderCache cache(1024);
    cache.store("a", four_words);

    CHECK(cache.find("a") == four_words);
    CHECK_FALSE(cache.find("b").has_value());
    CHECK(cache.stored_bytes() == 17);
}

TEST_CASE("the module that was used least recently leaves a full cache") {
    ShaderCache cache(two_modules);
    cache.store("a", four_words);
    cache.store("b", four_words);
    REQUIRE(cache.find("a").has_value());

    cache.store("c", four_words);

    CHECK(cache.find("a").has_value());
    CHECK_FALSE(cache.find("b").has_value());
    CHECK(cache.find("c").has_value());
    CHECK(cache.entry_count() == 2);
}

TEST_CASE("a module larger than the cache is not kept and a key stored again replaces its module") {
    ShaderCache cache(two_modules);
    cache.store("a", four_words);

    cache.store("large", std::vector<std::uint32_t>(64));
    cache.store("a", {9});

    CHECK_FALSE(cache.find("large").has_value());
    CHECK(cache.find("a") == std::vector<std::uint32_t>{9});
    CHECK(cache.stored_bytes() == 5);
}

TEST_CASE("the key changes with the stage, the compiler, the contract revision and the source") {
    CoreShaderBindings revised = scene_bindings();
    revised.revision += 1;
    const std::string key = shader_cache_key(Stage::vertex, "glslang 15", scene_bindings(), "void main() {}");

    CHECK(key == shader_cache_key(Stage::vertex, "glslang 15", scene_bindings(), "void main() {}"));
    CHECK(key != shader_cache_key(Stage::fragment, "glslang 15", scene_bindings(), "void main() {}"));
    CHECK(key != shader_cache_key(Stage::vertex, "glslang 16", scene_bindings(), "void main() {}"));
    CHECK(key != shader_cache_key(Stage::vertex, "glslang 15", revised, "void main() {}"));
    CHECK(key != shader_cache_key(Stage::vertex, "glslang 15", scene_bindings(), "void main() { }"));
}
