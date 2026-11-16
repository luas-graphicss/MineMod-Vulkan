#include "terrain/section_space.hpp"

namespace minemod::renderer {
namespace {

constexpr double half_section_edge = section_edge_blocks / 2.0;

double offset_from_camera(std::int32_t section_coordinate, double camera_coordinate) noexcept {
    return static_cast<double>(section_coordinate) * section_edge_blocks - camera_coordinate;
}

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
