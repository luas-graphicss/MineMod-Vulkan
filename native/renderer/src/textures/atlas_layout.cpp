#include "textures/atlas_layout.hpp"

#include <algorithm>
#include <cstddef>
#include <functional>
#include <numeric>
#include <stdexcept>
#include <utility>

namespace minemod::renderer {

namespace {

constexpr std::uint32_t max_mip_levels = 16;

struct AtlasSize {
    std::uint64_t width;
    std::uint64_t height;
};

std::uint64_t align_up(std::uint32_t texels, std::uint32_t alignment) {
    return (std::uint64_t{texels} + alignment - 1) / alignment * alignment;
}

std::vector<std::size_t> tallest_first(std::span<const VkExtent2D> sprites) {
    std::vector<std::size_t> order(sprites.size());
    std::iota(order.begin(), order.end(), std::size_t{0});
    std::ranges::stable_sort(order, std::ranges::greater{}, [&](std::size_t sprite) { return sprites[sprite].height; });
    return order;
}

std::optional<std::vector<SpriteRegion>> place_on_shelves(
    std::span<const VkExtent2D> sprites,
    std::span<const std::size_t> order,
    const AtlasSize& atlas,
    std::uint32_t alignment) {
    std::vector<SpriteRegion> regions(sprites.size());
    std::uint64_t shelf_x = 0;
    std::uint64_t shelf_y = 0;
    std::uint64_t shelf_height = 0;
    for (const std::size_t sprite : order) {
        const std::uint64_t cell_width = align_up(sprites[sprite].width, alignment);
        const std::uint64_t cell_height = align_up(sprites[sprite].height, alignment);
        if (shelf_x + cell_width > atlas.width) {
            shelf_x = 0;
            shelf_y += shelf_height;
            shelf_height = 0;
        }
        if (cell_width > atlas.width || shelf_y + cell_height > atlas.height) {
            return std::nullopt;
        }
        regions[sprite] = SpriteRegion{
            static_cast<std::uint32_t>(shelf_x),
            static_cast<std::uint32_t>(shelf_y),
            sprites[sprite].width,
            sprites[sprite].height,
        };
        shelf_x += cell_width;
        shelf_height = std::max(shelf_height, cell_height);
    }
    return regions;
}

}

std::optional<AtlasLayout> pack_atlas(std::span<const VkExtent2D> sprites, const AtlasLimits& limits) {
    if (limits.mip_levels == 0 || limits.mip_levels > max_mip_levels) {
        throw std::invalid_argument("an atlas needs between 1 and 16 mip levels");
    }
    const auto is_empty = [](const VkExtent2D& sprite) { return sprite.width == 0 || sprite.height == 0; };
    if (std::ranges::any_of(sprites, is_empty)) {
        throw std::invalid_argument("a sprite needs at least one texel");
    }

    const std::uint32_t alignment = std::uint32_t{1} << (limits.mip_levels - 1);
    const std::vector<std::size_t> order = tallest_first(sprites);
    AtlasSize atlas{alignment, alignment};
    while (atlas.width <= limits.max_extent && atlas.height <= limits.max_extent) {
        if (std::optional<std::vector<SpriteRegion>> regions = place_on_shelves(sprites, order, atlas, alignment)) {
            const VkExtent2D extent{static_cast<std::uint32_t>(atlas.width), static_cast<std::uint32_t>(atlas.height)};
            return AtlasLayout{extent, std::move(*regions)};
        }
        if (atlas.width <= atlas.height) {
            atlas.width *= 2;
        } else {
            atlas.height *= 2;
        }
    }
    return std::nullopt;
}

}
