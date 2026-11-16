#pragma once

#include <cstdint>
#include <vector>

#include "math/vector.hpp"
#include "terrain/section_mesh.hpp"

namespace minemod::renderer {

inline constexpr std::uint32_t indices_per_quad = 6;

struct TranslucentQuads {
    std::vector<Vector3> centers;
    std::vector<std::uint32_t> indices;
};

TranslucentQuads translucent_quads(const SectionMesh& mesh);

std::vector<std::uint32_t> far_to_near(const TranslucentQuads& quads, const Vector3& camera_in_section);

}
