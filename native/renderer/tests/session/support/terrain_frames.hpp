#pragma once

#include <array>
#include <cstdint>
#include <vector>

#include "session/support/running_session.hpp"
#include "session/support/scene_stream.hpp"

namespace minemod::scene::test_support {

inline constexpr std::array<float, 4> terrain_backdrop{32.0f / 255.0f, 32.0f / 255.0f, 32.0f / 255.0f, 1.0f};
inline constexpr Rgb terrain_backdrop_pixel{32, 32, 32};
inline constexpr std::array<double, 3> camera_before_the_sections{8.0, 8.0, 0.0};

std::vector<std::uint8_t> terrain_textures();
TerrainState terrain_seen_from(const std::array<double, 3>& camera = camera_before_the_sections);
std::vector<std::uint8_t> world_frame(
    SceneStream stream, const std::array<double, 3>& camera = camera_before_the_sections);

}
