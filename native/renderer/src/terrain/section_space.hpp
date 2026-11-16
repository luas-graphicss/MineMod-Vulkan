#pragma once

#include <cstdint>

#include "math/box.hpp"
#include "math/vector.hpp"
#include "work/job_epochs.hpp"

namespace minemod::renderer {

inline constexpr std::int32_t section_edge_blocks = 16;

SectionPosition section_containing(const WorldPosition& position);
Vector3 section_origin(const SectionPosition& section, const WorldPosition& camera) noexcept;
Box section_bounds(const SectionPosition& section, const WorldPosition& camera) noexcept;
double squared_distance_to_center(const SectionPosition& section, const WorldPosition& camera) noexcept;

}
