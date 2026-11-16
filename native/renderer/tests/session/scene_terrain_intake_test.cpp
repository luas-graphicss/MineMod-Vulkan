#include <array>
#include <cstddef>
#include <cstdint>
#include <vector>

#include <doctest.h>

#include "minemod/abi/scene_commands.h"
#include "minemod/scene/scene_session.h"
#include "session/support/running_session.hpp"
#include "session/support/scene_shapes.hpp"
#include "session/support/scene_stream.hpp"
#include "session/support/terrain_frames.hpp"
#include "support/terrain_quads.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/terrain_layer.hpp"

namespace {

using namespace minemod::scene::test_support;
namespace renderer = minemod::renderer;
using renderer::test_support::QuadPlacement;

constexpr VkExtent2D frame_extent{64, 64};
constexpr Rgb red_over_backdrop{144, 16, 16};
constexpr Rgb red_over_blue{136, 8, 72};
constexpr Color red{255, 0, 0, 255};
constexpr Color green{0, 255, 0, 255};
constexpr Color blue{0, 0, 255, 255};
constexpr Color half_red{255, 0, 0, 128};
constexpr Color half_blue{0, 0, 255, 128};
constexpr std::array<double, 3> camera_left_of_the_front_quad{2.0, 8.0, 0.0};
constexpr std::array<double, 3> camera_facing_the_front_quad{12.0, 8.0, 0.0};
constexpr SectionKey near_section{0, 0, -2, 0};
constexpr SectionKey middle_section{0, 0, -3, 0};
constexpr SectionKey far_section{0, 0, -4, 0};
constexpr QuadPlacement near_strip{3.2f, 0.0f, 6.4f, 16.0f, 8.0f};
constexpr QuadPlacement middle_strip{5.4f, 0.0f, 10.6f, 16.0f, 8.0f};
constexpr QuadPlacement far_strip{11.8f, 0.0f, 16.0f, 16.0f, 8.0f};
constexpr QuadPlacement wide_quad_behind{0.0f, 0.0f, 16.0f, 16.0f, 7.5f};
constexpr QuadPlacement right_quad_in_front{8.0f, 0.0f, 16.0f, 16.0f, 8.0f};
constexpr std::uint32_t near_column = 10;
constexpr std::uint32_t middle_column = 32;
constexpr std::uint32_t far_column = 48;
constexpr std::uint32_t right_edge_column = 60;
constexpr std::uint32_t middle_row = 32;
constexpr std::size_t quads_filling_most_of_the_arena = 18;
constexpr std::size_t quads_exceeding_the_arena = 31;

minemod_scene_session_config smallest_arena_config() {
    minemod_scene_session_config config = offscreen_config(frame_extent.width, frame_extent.height);
    config.upload_bytes_per_frame = MINEMOD_SCENE_MIN_ARENA_BYTES;
    return config;
}

renderer::SectionMesh strip_mesh(const QuadPlacement& strip, const Color& color, std::size_t quad_count) {
    renderer::SectionMesh mesh{};
    for (std::size_t quad = 0; quad < quad_count; ++quad) {
        renderer::test_support::append_quad(
            mesh, renderer::TerrainLayer::solid, strip, {color, {0.5f, 0.5f}, {240, 240}});
    }
    return mesh;
}

renderer::SectionMesh crowding_mesh(const QuadPlacement& strip, const Color& color) {
    return strip_mesh(strip, color, quads_filling_most_of_the_arena);
}

renderer::SectionMesh overlapping_water() {
    renderer::SectionMesh mesh{};
    renderer::test_support::append_quad(
        mesh, renderer::TerrainLayer::translucent, wide_quad_behind, {half_blue, {0.5f, 0.5f}, {240, 240}});
    renderer::test_support::append_quad(
        mesh, renderer::TerrainLayer::translucent, right_quad_in_front, {half_red, {0.5f, 0.5f}, {240, 240}});
    return mesh;
}

DrawnFrame drawn_by(RunningSession& session) {
    return session.read_frame(frame_extent.width, frame_extent.height);
}

void check_column(const DrawnFrame& frame, std::uint32_t column, const Rgb& expected) {
    CHECK_MESSAGE(frame.shows(column, middle_row, expected), frame.describe(column, middle_row));
}

}

TEST_CASE("sections that do not fit the upload arena of a frame wait for later frames, the nearest first") {
    RunningSession session(smallest_arena_config());
    REQUIRE(session.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    SceneStream crowded;
    crowded.publish_section(middle_section, crowding_mesh(middle_strip, green))
        .publish_section(far_section, crowding_mesh(far_strip, blue))
        .publish_section(near_section, crowding_mesh(near_strip, red));

    REQUIRE(session.submit(world_frame(crowded)) == MINEMOD_RESULT_OK);
    const DrawnFrame first = drawn_by(session);
    check_column(first, middle_column, {0, 255, 0});
    check_column(first, near_column, terrain_backdrop_pixel);
    check_column(first, far_column, terrain_backdrop_pixel);
    CHECK(session.counters().live_sections == 1);

    REQUIRE(session.submit(world_frame(SceneStream())) == MINEMOD_RESULT_OK);
    const DrawnFrame second = drawn_by(session);
    check_column(second, near_column, {255, 0, 0});
    check_column(second, far_column, terrain_backdrop_pixel);
    CHECK(session.counters().live_sections == 2);

    REQUIRE(session.submit(world_frame(SceneStream())) == MINEMOD_RESULT_OK);
    const DrawnFrame third = drawn_by(session);
    check_column(third, near_column, {255, 0, 0});
    check_column(third, middle_column, {0, 255, 0});
    check_column(third, far_column, {0, 0, 255});
    CHECK(session.counters().sections_published == 3);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a waiting section takes the newest mesh published for it") {
    RunningSession session(smallest_arena_config());
    REQUIRE(session.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    SceneStream crowded;
    crowded.publish_section(middle_section, crowding_mesh(middle_strip, green))
        .publish_section(far_section, crowding_mesh(far_strip, blue))
        .publish_section(far_section, crowding_mesh(far_strip, red));

    REQUIRE(session.submit(world_frame(crowded)) == MINEMOD_RESULT_OK);
    check_column(drawn_by(session), far_column, terrain_backdrop_pixel);

    REQUIRE(session.submit(world_frame(SceneStream())) == MINEMOD_RESULT_OK);
    check_column(drawn_by(session), far_column, {255, 0, 0});
    CHECK(session.counters().sections_published == 2);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a section unloaded while it waits is dropped instead of drawn") {
    RunningSession session(smallest_arena_config());
    REQUIRE(session.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    SceneStream crowded;
    crowded.publish_section(middle_section, crowding_mesh(middle_strip, green))
        .publish_section(far_section, crowding_mesh(far_strip, blue));
    REQUIRE(session.submit(world_frame(crowded)) == MINEMOD_RESULT_OK);
    SceneStream unloading;
    unloading.unload_section(far_section);

    REQUIRE(session.submit(world_frame(unloading)) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = drawn_by(session);
    check_column(frame, middle_column, {0, 255, 0});
    check_column(frame, far_column, terrain_backdrop_pixel);
    const minemod_scene_counters counters = session.counters();
    CHECK(counters.sections_dropped == 1);
    CHECK(counters.sections_published == 1);
    CHECK(counters.live_sections == 1);
    CHECK(session.validation_text() == "");
}

TEST_CASE("changing the world drops the sections that wait") {
    RunningSession session(smallest_arena_config());
    const auto stream = SceneStream()
                            .publish_section(middle_section, crowding_mesh(middle_strip, green))
                            .publish_section(far_section, crowding_mesh(far_strip, blue))
                            .change_world()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const minemod_scene_counters counters = session.counters();
    CHECK(counters.sections_dropped == 1);
    CHECK(counters.sections_published == 1);
    CHECK(counters.live_sections == 0);
}

TEST_CASE("a section mesh larger than the upload arena of a frame is refused") {
    RunningSession session(smallest_arena_config());
    const auto stream =
        SceneStream().publish_section(middle_section, strip_mesh(middle_strip, green, quads_exceeding_the_arena)).build();
    minemod_stream_report report{};

    CHECK(session.submit(stream, report) == MINEMOD_RESULT_LIMIT_EXCEEDED);
    CHECK(report.failed_command == 0);
}

TEST_CASE("waiting sections count toward the section limit, and a newer mesh for one of them does not") {
    minemod_scene_session_config config = smallest_arena_config();
    config.max_sections = 2;
    RunningSession session(config);
    const auto crowded = SceneStream()
                             .publish_section(middle_section, crowding_mesh(middle_strip, green))
                             .publish_section(far_section, crowding_mesh(far_strip, blue))
                             .publish_section(near_section, crowding_mesh(near_strip, red))
                             .build();
    minemod_stream_report report{};

    CHECK(session.submit(crowded, report) == MINEMOD_RESULT_LIMIT_EXCEEDED);
    CHECK(report.failed_command == 2);

    const auto remeshed = SceneStream().publish_section(far_section, crowding_mesh(far_strip, red)).build();
    CHECK(session.submit(remeshed) == MINEMOD_RESULT_OK);
}

TEST_CASE("translucent quads are drawn from the farthest again when the camera moves to another block") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(session.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    SceneStream loaded;
    loaded.publish_section(middle_section, overlapping_water());

    REQUIRE(session.submit(world_frame(loaded, camera_left_of_the_front_quad)) == MINEMOD_RESULT_OK);
    check_column(drawn_by(session), right_edge_column, red_over_backdrop);

    REQUIRE(session.submit(world_frame(SceneStream(), camera_facing_the_front_quad)) == MINEMOD_RESULT_OK);
    check_column(drawn_by(session), middle_column, red_over_blue);
    CHECK(session.validation_text() == "");
}
