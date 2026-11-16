#include <algorithm>
#include <array>
#include <cstddef>
#include <span>
#include <stdexcept>

#include <doctest.h>

#include "textures/atlas_layout.hpp"

namespace {

using namespace minemod::renderer;

constexpr std::array mixed_sprites{
    VkExtent2D{16, 16}, VkExtent2D{32, 16}, VkExtent2D{16, 32}, VkExtent2D{8, 8}, VkExtent2D{16, 16},
};

bool overlap(const SpriteRegion& left, const SpriteRegion& right) {
    return left.x < right.x + right.width && right.x < left.x + left.width && left.y < right.y + right.height
        && right.y < left.y + left.height;
}

bool has_overlap(const AtlasLayout& layout) {
    const std::span<const SpriteRegion> regions(layout.regions);
    for (std::size_t first = 0; first < regions.size(); ++first) {
        const auto overlaps_first = [&](const SpriteRegion& other) { return overlap(regions[first], other); };
        if (std::ranges::any_of(regions.subspan(first + 1), overlaps_first)) {
            return true;
        }
    }
    return false;
}

bool lies_inside(const AtlasLayout& layout) {
    return std::ranges::all_of(layout.regions, [&](const SpriteRegion& region) {
        return region.x + region.width <= layout.extent.width && region.y + region.height <= layout.extent.height;
    });
}

}

TEST_CASE("sprites keep their size and never overlap inside the atlas") {
    const AtlasLayout layout = pack_atlas(mixed_sprites, AtlasLimits{1024, 1}).value();

    REQUIRE(layout.regions.size() == mixed_sprites.size());
    for (std::size_t sprite = 0; sprite < mixed_sprites.size(); ++sprite) {
        CHECK(layout.regions[sprite].width == mixed_sprites[sprite].width);
        CHECK(layout.regions[sprite].height == mixed_sprites[sprite].height);
    }
    CHECK(lies_inside(layout));
    CHECK_FALSE(has_overlap(layout));
}

TEST_CASE("the atlas grows by powers of two until the sprites fit") {
    const std::array four_tiles{VkExtent2D{16, 16}, VkExtent2D{16, 16}, VkExtent2D{16, 16}, VkExtent2D{16, 16}};

    const AtlasLayout layout = pack_atlas(four_tiles, AtlasLimits{1024, 1}).value();

    CHECK(layout.extent.width == 32);
    CHECK(layout.extent.height == 32);
}

TEST_CASE("every region starts on a texel of the smallest mip level") {
    const std::array odd_sprites{VkExtent2D{6, 6}, VkExtent2D{5, 3}, VkExtent2D{9, 2}};

    const AtlasLayout layout = pack_atlas(odd_sprites, AtlasLimits{1024, 3}).value();

    for (const SpriteRegion& region : layout.regions) {
        CHECK(region.x % 4 == 0);
        CHECK(region.y % 4 == 0);
    }
    CHECK(lies_inside(layout));
    CHECK_FALSE(has_overlap(layout));
}

TEST_CASE("sprites that exceed the largest allowed atlas are refused") {
    const std::array two_large_sprites{VkExtent2D{64, 64}, VkExtent2D{64, 64}};

    CHECK_FALSE(pack_atlas(two_large_sprites, AtlasLimits{64, 1}).has_value());
}

TEST_CASE("a sprite without texels is rejected") {
    const std::array empty_sprite{VkExtent2D{0, 16}};

    CHECK_THROWS_AS(pack_atlas(empty_sprite, AtlasLimits{64, 1}), std::invalid_argument);
}
