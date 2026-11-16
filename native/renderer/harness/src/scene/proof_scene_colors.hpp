#pragma once

#include <array>

#include "frame/frame_readback.hpp"

namespace minemod::harness {

inline constexpr renderer::Pixel proof_clear_color{51, 51, 51, 255};
inline constexpr renderer::Pixel proof_triangle_color{255, 255, 0, 255};
inline constexpr std::array<renderer::Pixel, 4> checker_texels{
    renderer::Pixel{255, 0, 0, 255},
    renderer::Pixel{0, 255, 0, 255},
    renderer::Pixel{0, 0, 255, 255},
    renderer::Pixel{255, 255, 255, 255},
};

constexpr std::array<float, 4> unit_color(const renderer::Pixel& pixel) noexcept {
    constexpr float max_channel = 255.0f;
    return {
        static_cast<float>(pixel.red) / max_channel,
        static_cast<float>(pixel.green) / max_channel,
        static_cast<float>(pixel.blue) / max_channel,
        static_cast<float>(pixel.alpha) / max_channel,
    };
}

}
