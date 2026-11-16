#pragma once

#include <span>
#include <vector>

#include "math/vector.hpp"
#include "terrain/terrain_layer.hpp"
#include "visibility/frustum.hpp"
#include "work/job_epochs.hpp"

namespace minemod::renderer {

std::vector<SectionPosition> visible_sections(
    std::span<const SectionPosition> sections, const Frustum& frustum, const WorldPosition& camera);

std::vector<SectionPosition> ordered_sections(
    std::span<const SectionPosition> sections, const WorldPosition& camera, DrawDirection direction);

}
