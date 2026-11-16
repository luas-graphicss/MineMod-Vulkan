#include <array>
#include <numbers>
#include <vector>

#include <doctest.h>

#include "math/matrix.hpp"
#include "terrain/section_selection.hpp"
#include "visibility/frustum.hpp"

namespace {

using namespace minemod::renderer;

constexpr float half_turn = std::numbers::pi_v<float>;
constexpr PerspectiveLens lens{half_turn / 2.0f, 1.0f, 0.5f, 256.0f};
constexpr WorldPosition camera{8.0, 8.0, 8.0};

constexpr SectionPosition ahead{0, 0, -2};
constexpr SectionPosition far_ahead{0, 0, -4};
constexpr SectionPosition behind{0, 0, 2};
constexpr SectionPosition just_behind{0, 0, 1};
constexpr SectionPosition left{-1, 0, 0};
constexpr SectionPosition right{1, 0, 0};

}

TEST_CASE("sections outside the view are not selected") {
    const std::array sections{ahead, behind};

    const std::vector<SectionPosition> visible = visible_sections(sections, Frustum(perspective(lens)), camera);

    CHECK(visible == std::vector{ahead});
}

TEST_CASE("turning around selects the sections that were behind the camera") {
    const std::array sections{ahead, behind};
    const Frustum turned(multiply(perspective(lens), rotation_about_y(half_turn)));

    CHECK(visible_sections(sections, turned, camera) == std::vector{behind});
}

TEST_CASE("sections are ordered from the nearest or from the farthest as the layer asks") {
    const std::array sections{far_ahead, just_behind, ahead};

    CHECK(ordered_sections(sections, camera, DrawDirection::front_to_back) == std::vector{just_behind, ahead, far_ahead});
    CHECK(ordered_sections(sections, camera, DrawDirection::back_to_front) == std::vector{far_ahead, ahead, just_behind});
}

TEST_CASE("sections at the same distance keep one order no matter how they arrive") {
    const std::array left_first{left, right};
    const std::array right_first{right, left};

    for (const DrawDirection direction : {DrawDirection::front_to_back, DrawDirection::back_to_front}) {
        CHECK(ordered_sections(left_first, camera, direction) == std::vector{left, right});
        CHECK(ordered_sections(right_first, camera, direction) == std::vector{left, right});
    }
}
