#include "textures/mip_chain.hpp"

#include <algorithm>
#include <array>
#include <bit>
#include <cmath>
#include <initializer_list>
#include <stdexcept>

namespace minemod::renderer {

namespace {

constexpr std::size_t channel_count = 4;
constexpr std::size_t alpha_channel = 3;
constexpr float display_gamma = 2.2f;
constexpr std::uint8_t cutout_alpha_floor = 96;

using Texel = std::array<std::byte, channel_count>;
using TexelLight = std::array<float, channel_count>;

std::size_t texel_bytes(VkExtent2D extent) {
    return std::size_t{extent.width} * extent.height * channel_count;
}

std::array<float, 256> linear_light_table() {
    std::array<float, 256> table{};
    for (std::size_t encoded = 0; encoded < table.size(); ++encoded) {
        table[encoded] = std::pow(static_cast<float>(encoded) / 255.0f, display_gamma);
    }
    return table;
}

float to_linear_light(std::byte encoded) {
    static const std::array<float, 256> table = linear_light_table();
    return table[std::to_integer<std::size_t>(encoded)];
}

std::byte to_encoded(float linear_light) {
    const long encoded = std::lround(std::pow(linear_light, 1.0f / display_gamma) * 255.0f);
    return static_cast<std::byte>(static_cast<std::uint8_t>(encoded));
}

bool has_transparent_texel(std::span<const std::byte> rgba) {
    for (std::size_t alpha = alpha_channel; alpha < rgba.size(); alpha += channel_count) {
        if (rgba[alpha] == std::byte{0}) {
            return true;
        }
    }
    return false;
}

struct LevelTexels {
    std::span<const std::byte> rgba;
    VkExtent2D extent;

    std::span<const std::byte> texel(std::uint32_t x, std::uint32_t y) const {
        const std::size_t column = std::min(x, extent.width - 1);
        const std::size_t row = std::min(y, extent.height - 1);
        return rgba.subspan((row * extent.width + column) * channel_count, channel_count);
    }
};

void add_light(TexelLight& light, std::span<const std::byte> texel) {
    for (std::size_t channel = 0; channel < channel_count; ++channel) {
        light[channel] += to_linear_light(texel[channel]);
    }
}

Texel blend_block(const LevelTexels& larger, std::uint32_t x, std::uint32_t y, bool is_cutout) {
    TexelLight light{};
    for (const std::uint32_t row : {y * 2, y * 2 + 1}) {
        for (const std::uint32_t column : {x * 2, x * 2 + 1}) {
            const std::span<const std::byte> texel = larger.texel(column, row);
            if (!is_cutout || texel[alpha_channel] != std::byte{0}) {
                add_light(light, texel);
            }
        }
    }

    Texel blended{};
    for (std::size_t channel = 0; channel < channel_count; ++channel) {
        blended[channel] = to_encoded(light[channel] / 4.0f);
    }
    if (is_cutout && std::to_integer<std::uint8_t>(blended[alpha_channel]) < cutout_alpha_floor) {
        blended[alpha_channel] = std::byte{0};
    }
    return blended;
}

void halve(const LevelTexels& larger, std::span<std::byte> smaller, VkExtent2D smaller_extent, bool is_cutout) {
    auto next = smaller.begin();
    for (std::uint32_t y = 0; y < smaller_extent.height; ++y) {
        for (std::uint32_t x = 0; x < smaller_extent.width; ++x) {
            next = std::ranges::copy(blend_block(larger, x, y, is_cutout), next).out;
        }
    }
}

}

std::span<const std::byte> MipChain::level_bytes(std::size_t level) const {
    const MipLevel& mip = levels.at(level);
    return std::span(rgba).subspan(mip.offset, texel_bytes(mip.extent));
}

std::uint32_t full_mip_level_count(VkExtent2D extent) {
    if (extent.width == 0 || extent.height == 0) {
        throw std::invalid_argument("a texture needs at least one texel");
    }
    return static_cast<std::uint32_t>(std::bit_width(std::max(extent.width, extent.height)));
}

MipChain build_mip_chain(VkExtent2D extent, std::span<const std::byte> rgba, std::uint32_t level_count) {
    if (level_count == 0 || level_count > full_mip_level_count(extent)) {
        throw std::invalid_argument("the texture extent does not allow that many mip levels");
    }
    if (rgba.size() != texel_bytes(extent)) {
        throw std::invalid_argument("the texel bytes do not match the texture extent");
    }

    const bool is_cutout = has_transparent_texel(rgba);
    MipChain chain{{MipLevel{extent, 0}}, std::vector<std::byte>(rgba.begin(), rgba.end())};
    while (chain.levels.size() < level_count) {
        const MipLevel larger = chain.levels.back();
        const VkExtent2D halved{
            std::max(larger.extent.width / 2, std::uint32_t{1}),
            std::max(larger.extent.height / 2, std::uint32_t{1}),
        };
        const MipLevel smaller{halved, chain.rgba.size()};
        chain.rgba.resize(smaller.offset + texel_bytes(smaller.extent));

        const std::span<std::byte> texels(chain.rgba);
        const LevelTexels source{texels.subspan(larger.offset, texel_bytes(larger.extent)), larger.extent};
        halve(source, texels.subspan(smaller.offset), smaller.extent, is_cutout);
        chain.levels.push_back(smaller);
    }
    return chain;
}

}
