#include "terrain/translucent_sections.hpp"

#include <cmath>
#include <stdexcept>
#include <utility>

#include "terrain/section_selection.hpp"
#include "terrain/section_space.hpp"
#include "terrain/terrain_layer.hpp"

namespace minemod::renderer {
namespace {

constexpr std::size_t fewest_quads_worth_ordering = 2;

Vector3 camera_inside(const SectionPosition& section, const WorldPosition& camera) noexcept {
    const Vector3 origin = section_origin(section, camera);
    return Vector3{-origin.x, -origin.y, -origin.z};
}

}

void TranslucentSections::track(const SectionPosition& section, TranslucentQuads quads) {
    if (quads.centers.size() < fewest_quads_worth_ordering) {
        sections_.erase(section);
        return;
    }
    sections_.insert_or_assign(section, Tracked{std::move(quads), std::nullopt});
}

void TranslucentSections::forget(const SectionPosition& section) {
    sections_.erase(section);
}

void TranslucentSections::clear() noexcept {
    sections_.clear();
}

std::vector<SectionPosition> TranslucentSections::out_of_order(const WorldPosition& camera) const {
    const CameraBlock block = block_containing(camera);
    std::vector<SectionPosition> waiting;
    for (const auto& [section, tracked] : sections_) {
        if (tracked.ordered_for != block) {
            waiting.push_back(section);
        }
    }
    return ordered_sections(waiting, camera, DrawDirection::front_to_back);
}

std::vector<std::uint32_t> TranslucentSections::reorder(const SectionPosition& section, const WorldPosition& camera) {
    const auto tracked = sections_.find(section);
    if (tracked == sections_.end()) {
        throw std::invalid_argument("the section has no translucent quads to reorder");
    }
    tracked->second.ordered_for = block_containing(camera);
    return far_to_near(tracked->second.quads, camera_inside(section, camera));
}

TranslucentSections::CameraBlock TranslucentSections::block_containing(const WorldPosition& camera) noexcept {
    return CameraBlock{std::floor(camera.x), std::floor(camera.y), std::floor(camera.z)};
}

}
