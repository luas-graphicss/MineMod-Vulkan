#include "terrain/reachable_sections.hpp"

#include "terrain/section_space.hpp"

namespace minemod::scene {

std::optional<std::vector<renderer::SectionPosition>> reachable_sections(
    const renderer::SectionOcclusion& occlusion,
    const renderer::TerrainSections& sections,
    const renderer::WorldPosition& camera) {
    if (occlusion.size() == 0) {
        return std::nullopt;
    }

    std::vector<renderer::SectionPosition> reachable = occlusion.reachable_from(renderer::section_containing(camera));
    for (const renderer::SectionPosition& published : sections.positions()) {
        if (!occlusion.describes(published)) {
            reachable.push_back(published);
        }
    }
    return reachable;
}

}
