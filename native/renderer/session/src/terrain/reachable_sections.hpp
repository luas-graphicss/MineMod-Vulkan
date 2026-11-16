#pragma once

#include <optional>
#include <vector>

#include "math/vector.hpp"
#include "terrain/terrain_sections.hpp"
#include "visibility/section_occlusion.hpp"
#include "work/job_epochs.hpp"

namespace minemod::scene {

std::optional<std::vector<renderer::SectionPosition>> reachable_sections(
    const renderer::SectionOcclusion& occlusion,
    const renderer::TerrainSections& sections,
    const renderer::WorldPosition& camera);

}
