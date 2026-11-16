#pragma once

#include <algorithm>
#include <array>
#include <cmath>
#include <cstdint>

#include "math/vector.hpp"

namespace minemod::renderer {

enum class FogShape : std::uint8_t { sphere, cylinder };

struct Fog {
    std::array<float, 4> color;
    float start;
    float end;
    FogShape shape = FogShape::sphere;
};

constexpr bool has_depth(const Fog& fog) noexcept {
    return fog.end > fog.start;
}

constexpr float fog_factor(const Fog& fog, float distance) noexcept {
    const float progress = std::clamp((distance - fog.start) / (fog.end - fog.start), 0.0f, 1.0f);
    return progress * progress * (3.0f - 2.0f * progress);
}

inline float fog_distance(FogShape shape, const Vector3& from_camera) noexcept {
    if (shape == FogShape::cylinder) {
        return std::max(std::hypot(from_camera.x, from_camera.z), std::abs(from_camera.y));
    }
    return std::hypot(from_camera.x, from_camera.y, from_camera.z);
}

}
