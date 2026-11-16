#include <doctest.h>

#include "terrain/section_space.hpp"

namespace {

using namespace minemod::renderer;

}

TEST_CASE("a section origin is measured from the camera") {
    const Vector3 origin = section_origin({1, -2, 3}, {4.0, 5.0, 6.0});

    CHECK(origin == Vector3{12.0f, -37.0f, 42.0f});
}

TEST_CASE("camera-relative origins stay exact at the edge of the world") {
    const SectionPosition border_section{1875000, 0, -1875000};
    const WorldPosition camera{30000000.5, 0.25, -29999999.75};

    const Vector3 origin = section_origin(border_section, camera);

    CHECK(origin == Vector3{-0.5f, -0.25f, -0.25f});
}

TEST_CASE("section bounds span one section edge from the origin") {
    const Box bounds = section_bounds({0, 0, 0}, {0.0, 0.0, 0.0});

    CHECK(bounds.minimum == Vector3{0.0f, 0.0f, 0.0f});
    CHECK(bounds.maximum == Vector3{16.0f, 16.0f, 16.0f});
}

TEST_CASE("the distance to a section is measured to its center") {
    const WorldPosition first_section_center{8.0, 8.0, 8.0};

    CHECK(squared_distance_to_center({0, 0, 0}, first_section_center) == 0.0);
    CHECK(squared_distance_to_center({1, 0, 0}, first_section_center) == 256.0);
}
