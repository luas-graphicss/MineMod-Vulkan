#pragma once

#include <array>

#include "scene/proof_scene_colors.hpp"

namespace minemod::harness {

struct ProofVertex {
    std::array<float, 3> position;
    std::array<float, 2> texture_coordinate;
    std::array<float, 4> tint;
};

inline constexpr float near_depth = 0.25f;
inline constexpr float far_depth = 0.75f;
inline constexpr float triangle_reach = 0.25f;
inline constexpr float quad_reach = 0.75f;
inline constexpr std::array<float, 4> triangle_tint = unit_color(proof_triangle_color);
inline constexpr std::array<float, 4> texture_only{0.0f, 0.0f, 0.0f, 0.0f};

inline constexpr std::array<ProofVertex, 9> proof_vertices{{
    {{-triangle_reach, -triangle_reach, near_depth}, {0.0f, 0.0f}, triangle_tint},
    {{triangle_reach, -triangle_reach, near_depth}, {0.0f, 0.0f}, triangle_tint},
    {{0.0f, triangle_reach, near_depth}, {0.0f, 0.0f}, triangle_tint},
    {{-quad_reach, -quad_reach, far_depth}, {0.0f, 0.0f}, texture_only},
    {{quad_reach, -quad_reach, far_depth}, {1.0f, 0.0f}, texture_only},
    {{-quad_reach, quad_reach, far_depth}, {0.0f, 1.0f}, texture_only},
    {{quad_reach, -quad_reach, far_depth}, {1.0f, 0.0f}, texture_only},
    {{quad_reach, quad_reach, far_depth}, {1.0f, 1.0f}, texture_only},
    {{-quad_reach, quad_reach, far_depth}, {0.0f, 1.0f}, texture_only},
}};

}
