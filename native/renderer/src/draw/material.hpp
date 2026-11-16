#pragma once

#include <cstdint>

#include "pipelines/graphics_pipeline_state.hpp"

namespace minemod::renderer {

enum class FaceCulling : std::uint8_t { back_faces, disabled };

struct Shading {
    bool samples_texture;
    bool applies_lightmap;
    bool applies_overlay;
    bool shades_by_normal;
    bool fades_into_fog;
    bool widens_lines;

    friend bool operator==(const Shading&, const Shading&) = default;
};

constexpr std::uint32_t shading_bits(const Shading& shading) noexcept {
    return (shading.samples_texture ? 1u : 0u) | (shading.applies_lightmap ? 2u : 0u)
        | (shading.applies_overlay ? 4u : 0u) | (shading.shades_by_normal ? 8u : 0u)
        | (shading.fades_into_fog ? 16u : 0u) | (shading.widens_lines ? 32u : 0u);
}

struct Material {
    Blending blending;
    DepthState depth;
    FaceCulling culling;
    float alpha_cutoff;
    Shading shading;
    DepthBias depth_bias{};
    bool writes_color = true;

    friend bool operator==(const Material&, const Material&) = default;
};

}
