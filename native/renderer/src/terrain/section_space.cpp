#include "terrain/section_space.hpp"

#include <algorithm>
#include <cmath>
#include <limits>
#include <stdexcept>

namespace minemod::renderer {
namespace {

constexpr double half_section_edge = section_edge_blocks / 2.0;
constexpr double lowest_section_coordinate = std::numeric_limits<std::int32_t>::min();
constexpr double highest_section_coordinate = std::numeric_limits<std::int32_t>::max();

std::int32_t section_coordinate(double block_coordinate) {
    if (!std::isfinite(block_coordinate)) {
        throw std::invalid_argument("a world position must be finite to lie in a section");
    }
    const double section = std::floor(block_coordinate / section_edge_blocks);
    return static_cast<std::int32_t>(std::clamp(section, lowest_section_coordinate, highest_section_coordinate));
}

double offset_from_camera(std::int32_t section_coordinate, double camera_coordinate) noexcept {
    return static_cast<double>(section_coordinate) * section_edge_blocks - camera_coordinate;
}

}

SectionPosition section_containing(const WorldPosition& position) {
    return SectionPosition{
        section_coordinate(position.x), section_coordinate(position.y), section_coordinate(position.z)};
}

Vector3 section_origin(const SectionPosition& section, const WorldPosition& camera) noexcept {
    return Vector3{
        static_cast<float>(offset_from_camera(section.x, camera.x)),
        static_cast<float>(offset_from_camera(section.y, camera.y)),
        static_cast<float>(offset_from_camera(section.z, camera.z)),
    };
}

Box section_bounds(const SectionPosition& section, const WorldPosition& camera) noexcept {
    const Vector3 origin = section_origin(section, camera);
    const auto edge = static_cast<float>(section_edge_blocks);
    return Box{origin, Vector3{origin.x + edge, origin.y + edge, origin.z + edge}};
}

double squared_distance_to_center(const SectionPosition& section, const WorldPosition& camera) noexcept {
    const double x = offset_from_camera(section.x, camera.x) + half_section_edge;
    const double y = offset_from_camera(section.y, camera.y) + half_section_edge;
    const double z = offset_from_camera(section.z, camera.z) + half_section_edge;
    return x * x + y * y + z * z;
}

}
