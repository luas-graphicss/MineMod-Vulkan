#include "terrain/section_selection.hpp"

#include <algorithm>
#include <iterator>
#include <tuple>

#include "terrain/section_space.hpp"

namespace minemod::renderer {
namespace {

bool precedes_by_coordinates(const SectionPosition& left, const SectionPosition& right) noexcept {
    return std::tie(left.x, left.y, left.z) < std::tie(right.x, right.y, right.z);
}

}

std::vector<SectionPosition> visible_sections(
    std::span<const SectionPosition> sections, const Frustum& frustum, const WorldPosition& camera) {
    std::vector<SectionPosition> visible;
    std::ranges::copy_if(sections, std::back_inserter(visible), [&](const SectionPosition& section) {
        return frustum.intersects(section_bounds(section, camera));
    });
    return visible;
}

std::vector<SectionPosition> ordered_sections(
    std::span<const SectionPosition> sections, const WorldPosition& camera, DrawDirection direction) {
    std::vector<SectionPosition> ordered(sections.begin(), sections.end());
    std::ranges::sort(ordered, [&](const SectionPosition& left, const SectionPosition& right) {
        const double left_distance = squared_distance_to_center(left, camera);
        const double right_distance = squared_distance_to_center(right, camera);
        if (left_distance == right_distance) {
            return precedes_by_coordinates(left, right);
        }
        return direction == DrawDirection::front_to_back ? left_distance < right_distance
                                                         : left_distance > right_distance;
    });
    return ordered;
}

}
