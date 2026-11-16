#pragma once

#include <algorithm>
#include <array>

namespace minemod::renderer {

struct Fog {
    std::array<float, 4> color;
    float start;
    float end;
};

constexpr bool has_depth(const Fog& fog) noexcept {
    return fog.end > fog.start;
}

constexpr float fog_factor(const Fog& fog, float distance) noexcept {
    return std::clamp((distance - fog.start) / (fog.end - fog.start), 0.0f, 1.0f);
}

}
