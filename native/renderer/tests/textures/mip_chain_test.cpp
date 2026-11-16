#include <array>
#include <cstddef>
#include <cstdint>
#include <initializer_list>
#include <span>
#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "textures/mip_chain.hpp"

namespace {

using namespace minemod::renderer;

using Rgba = std::array<std::uint8_t, 4>;

constexpr Rgba white{255, 255, 255, 255};
constexpr Rgba black{0, 0, 0, 255};
constexpr Rgba red{255, 0, 0, 255};
constexpr Rgba clear_green{0, 255, 0, 0};
constexpr Rgba clear_black{0, 0, 0, 0};

std::vector<std::byte> texels(std::initializer_list<Rgba> colors) {
    std::vector<std::byte> bytes;
    for (const Rgba& color : colors) {
        for (const std::uint8_t channel : color) {
            bytes.push_back(std::byte{channel});
        }
    }
    return bytes;
}

Rgba first_texel(std::span<const std::byte> level) {
    return Rgba{
        std::to_integer<std::uint8_t>(level[0]),
        std::to_integer<std::uint8_t>(level[1]),
        std::to_integer<std::uint8_t>(level[2]),
        std::to_integer<std::uint8_t>(level[3]),
    };
}

}

TEST_CASE("each level halves the previous one down to a single texel") {
    const std::vector<std::byte> base(4 * 2 * 4, std::byte{0xff});

    const MipChain chain = build_mip_chain({4, 2}, base, full_mip_level_count({4, 2}));

    REQUIRE(chain.levels.size() == 3);
    CHECK(chain.levels[1].extent.width == 2);
    CHECK(chain.levels[1].extent.height == 1);
    CHECK(chain.levels[2].extent.width == 1);
    CHECK(chain.levels[2].extent.height == 1);
    CHECK(chain.rgba.size() == (8 + 2 + 1) * 4);
}

TEST_CASE("a uniform color survives every level") {
    const Rgba teal{12, 200, 180, 255};

    const MipChain chain = build_mip_chain({2, 2}, texels({teal, teal, teal, teal}), 2);

    CHECK(first_texel(chain.level_bytes(1)) == teal);
}

TEST_CASE("opaque texels are averaged in linear light") {
    const MipChain chain = build_mip_chain({2, 2}, texels({white, white, black, black}), 2);

    CHECK(first_texel(chain.level_bytes(1)) == Rgba{186, 186, 186, 255});
}

TEST_CASE("transparent texels do not tint the opaque texel they are blended with") {
    const MipChain chain = build_mip_chain({2, 2}, texels({red, clear_green, clear_green, clear_green}), 2);

    CHECK(first_texel(chain.level_bytes(1)) == Rgba{136, 0, 0, 136});
}

TEST_CASE("a block that is mostly transparent becomes fully transparent") {
    const Rgba faint_white{255, 255, 255, 100};

    const MipChain chain = build_mip_chain({2, 2}, texels({faint_white, clear_black, clear_black, clear_black}), 2);

    CHECK(first_texel(chain.level_bytes(1))[3] == 0);
}

TEST_CASE("mip levels beyond the extent and mismatched texel bytes are rejected") {
    const std::vector<std::byte> base(2 * 2 * 4);

    CHECK_THROWS_AS(build_mip_chain({2, 2}, base, 3), std::invalid_argument);
    CHECK_THROWS_AS(build_mip_chain({2, 2}, base, 0), std::invalid_argument);
    CHECK_THROWS_AS(build_mip_chain({4, 2}, base, 1), std::invalid_argument);
    CHECK_THROWS_AS(full_mip_level_count({0, 4}), std::invalid_argument);
}
