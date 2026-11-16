#pragma once

#include <optional>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

enum class Blending { disabled, alpha, additive, lightning, glint, crumbling, inversion, darkening };

struct BlendFactors {
    VkBlendFactor source_color;
    VkBlendFactor destination_color;
    VkBlendFactor source_alpha;
    VkBlendFactor destination_alpha;

    friend bool operator==(const BlendFactors&, const BlendFactors&) = default;
};

constexpr std::optional<BlendFactors> blend_factors(Blending blending) noexcept {
    switch (blending) {
    case Blending::alpha:
        return BlendFactors{
            VK_BLEND_FACTOR_SRC_ALPHA,
            VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA,
            VK_BLEND_FACTOR_ONE,
            VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA,
        };
    case Blending::additive:
        return BlendFactors{VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ONE};
    case Blending::lightning:
        return BlendFactors{
            VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_SRC_ALPHA, VK_BLEND_FACTOR_ONE};
    case Blending::glint:
        return BlendFactors{
            VK_BLEND_FACTOR_SRC_COLOR, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO, VK_BLEND_FACTOR_ONE};
    case Blending::crumbling:
        return BlendFactors{
            VK_BLEND_FACTOR_DST_COLOR, VK_BLEND_FACTOR_SRC_COLOR, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO};
    case Blending::inversion:
        return BlendFactors{
            VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR,
            VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR,
            VK_BLEND_FACTOR_ONE,
            VK_BLEND_FACTOR_ZERO,
        };
    case Blending::darkening:
        return BlendFactors{
            VK_BLEND_FACTOR_ZERO, VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR, VK_BLEND_FACTOR_ONE, VK_BLEND_FACTOR_ZERO};
    case Blending::disabled:
        break;
    }
    return std::nullopt;
}

}
