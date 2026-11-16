#include <stdint.h>

#include <array>
#include <cstdint>
#include <vector>

#include <doctest.h>

#include "shaders/shader_interface.hpp"
#include "shaders/spirv_module.hpp"
#include "sprite_frag.h"
#include "sprite_vert.h"

using namespace minemod::renderer;

TEST_CASE("the sprite vertex shader reads three attributes and writes two varyings") {
    const ShaderInterface interface = reflect_interface(SpirvModule{sprite_vert});

    CHECK(interface.stage == ShaderStage::vertex);
    CHECK(interface.input_locations == std::vector<std::uint32_t>{0, 1, 2});
    CHECK(interface.output_locations == std::vector<std::uint32_t>{0, 1});
    CHECK(interface.resources.empty());
}

TEST_CASE("the sprite fragment shader samples one texture and writes one color") {
    const ShaderInterface interface = reflect_interface(SpirvModule{sprite_frag});

    CHECK(interface.stage == ShaderStage::fragment);
    CHECK(interface.input_locations == std::vector<std::uint32_t>{0, 1});
    CHECK(interface.output_locations == std::vector<std::uint32_t>{0});
    CHECK(interface.resources == std::vector<ResourceBinding>{{0, 0}});
}

TEST_CASE("a module without an entry point has no interface") {
    const std::array<std::uint32_t, 5> empty_module{0x07230203, 0x00010600, 0, 1, 0};

    CHECK_THROWS_AS(reflect_interface(SpirvModule{empty_module}), ShaderError);
}

TEST_CASE("a decoration that names an id beyond the module bound is rejected") {
    const std::array<std::uint32_t, 9> stray_decoration{
        0x07230203, 0x00010600, 0, 4, 0, (4u << 16) | 71u, 9, 30, 0,
    };

    CHECK_THROWS_AS(reflect_interface(SpirvModule{stray_decoration}), ShaderError);
}
