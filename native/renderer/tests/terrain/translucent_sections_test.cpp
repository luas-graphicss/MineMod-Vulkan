#include <cstdint>
#include <initializer_list>
#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "math/vector.hpp"
#include "support/terrain_quads.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/translucent_order.hpp"
#include "terrain/translucent_sections.hpp"
#include "work/job_epochs.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr SectionPosition near_section{0, 0, -1};
constexpr SectionPosition far_section{0, 0, -4};
constexpr WorldPosition camera{8.0, 8.0, 0.5};
constexpr WorldPosition camera_in_same_block{8.4, 8.0, 0.9};
constexpr WorldPosition camera_in_next_block{9.2, 8.0, 0.5};
constexpr QuadSurface white{{255, 255, 255, 255}, {0.0f, 0.0f}, {brightest_packed_light, brightest_packed_light}};
constexpr std::uint32_t first_corner_of_far_pane = 4;

TranslucentQuads panes_at(std::initializer_list<float> depths) {
    SectionMesh mesh{};
    for (const float depth : depths) {
        append_quad(mesh, TerrainLayer::translucent, {0.0f, 0.0f, 16.0f, 16.0f, depth}, white);
    }
    return translucent_quads(mesh);
}

TranslucentQuads near_then_far_pane() {
    return panes_at({12.0f, 4.0f});
}

}

TEST_CASE("a section is out of order until it is ordered for the block the camera is in") {
    TranslucentSections sections;
    sections.track(near_section, near_then_far_pane());
    REQUIRE(sections.out_of_order(camera).size() == 1);
    CHECK(sections.out_of_order(camera).front() == near_section);

    const std::vector<std::uint32_t> ordered = sections.reorder(near_section, camera);

    CHECK(ordered.front() == first_corner_of_far_pane);
    CHECK(sections.out_of_order(camera).empty());
    CHECK(sections.out_of_order(camera_in_same_block).empty());
}

TEST_CASE("a camera that enters another block puts ordered sections out of order again") {
    TranslucentSections sections;
    sections.track(near_section, near_then_far_pane());
    sections.reorder(near_section, camera);

    CHECK(sections.out_of_order(camera_in_next_block).size() == 1);
}

TEST_CASE("a new mesh of an ordered section is out of order again") {
    TranslucentSections sections;
    sections.track(near_section, near_then_far_pane());
    sections.reorder(near_section, camera);

    sections.track(near_section, near_then_far_pane());

    CHECK(sections.out_of_order(camera).size() == 1);
}

TEST_CASE("sections that are out of order are listed from the nearest") {
    TranslucentSections sections;
    sections.track(far_section, near_then_far_pane());
    sections.track(near_section, near_then_far_pane());

    const std::vector<SectionPosition> waiting = sections.out_of_order(camera);

    REQUIRE(waiting.size() == 2);
    CHECK(waiting.front() == near_section);
    CHECK(waiting.back() == far_section);
}

TEST_CASE("a section with fewer than two translucent quads has nothing to order") {
    TranslucentSections sections;
    sections.track(near_section, panes_at({4.0f}));
    CHECK(sections.size() == 0);

    sections.track(near_section, near_then_far_pane());
    sections.track(near_section, panes_at({4.0f}));
    CHECK(sections.size() == 0);
}

TEST_CASE("forgotten sections are no longer ordered") {
    TranslucentSections sections;
    sections.track(near_section, near_then_far_pane());
    sections.track(far_section, near_then_far_pane());

    sections.forget(near_section);
    CHECK(sections.size() == 1);
    CHECK_THROWS_AS(sections.reorder(near_section, camera), std::invalid_argument);

    sections.clear();
    CHECK(sections.out_of_order(camera).empty());
}
