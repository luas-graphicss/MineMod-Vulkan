#include <cstdint>

#include <doctest.h>

#include "minemod/abi/scene_commands.h"
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

constexpr VkExtent2D frame_extent{64, 64};
constexpr Color red{255, 0, 0, 255};
constexpr Rgb wall_pixel{255, 0, 0};
constexpr std::uint64_t open_faces =
    (std::uint64_t{1} << (MINEMOD_SECTION_FACE_COUNT * MINEMOD_SECTION_FACE_COUNT)) - 1;
constexpr std::uint64_t closed_faces = 0;
constexpr std::uint64_t pair_past_the_faces = open_faces + 1;
constexpr SectionKey camera_section{0, 0, 0, 0};
constexpr SectionKey section_before_the_camera{0, 0, -1, 0};
constexpr SectionKey section_before_the_wall{0, 0, -2, 0};
constexpr SectionKey wall_section{0, 0, -3, 0};
constexpr SectionKey wall_section_of_the_next_world{0, 0, -3, 1};
constexpr renderer::test_support::QuadPlacement wall_filling_the_view{0.0f, 0.0f, 16.0f, 16.0f, 8.0f};
constexpr std::uint32_t center = 32;

renderer::SectionMesh wall_mesh() {
    renderer::SectionMesh mesh{};
    renderer::test_support::append_quad(
        mesh, renderer::TerrainLayer::solid, wall_filling_the_view, {red, {0.5f, 0.5f}, {240, 240}});
    return mesh;
}

SceneStream tunnel_to_the_wall(std::uint64_t faces_before_the_camera) {
    SceneStream stream;
    stream.connect_section_faces(camera_section, open_faces)
        .connect_section_faces(section_before_the_camera, faces_before_the_camera)
        .connect_section_faces(section_before_the_wall, open_faces)
        .connect_section_faces(wall_section, open_faces);
    return stream;
}

void check_center(RunningSession& session, const Rgb& expected) {
    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    CHECK_MESSAGE(frame.shows(center, center, expected), frame.describe(center, center));
}

}

TEST_CASE("terrain behind a section with closed faces is hidden until those faces connect") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(session.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    SceneStream hidden = tunnel_to_the_wall(closed_faces);
    hidden.publish_section(wall_section, wall_mesh());

    REQUIRE(session.submit(world_frame(hidden)) == MINEMOD_RESULT_OK);
    check_center(session, terrain_backdrop_pixel);

    REQUIRE(session.submit(world_frame(tunnel_to_the_wall(open_faces))) == MINEMOD_RESULT_OK);
    check_center(session, wall_pixel);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a published section whose faces were never described is drawn whatever the others hide") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(session.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    SceneStream stream;
    stream.connect_section_faces(camera_section, open_faces)
        .connect_section_faces(section_before_the_camera, closed_faces)
        .publish_section(wall_section, wall_mesh());

    REQUIRE(session.submit(world_frame(stream)) == MINEMOD_RESULT_OK);

    check_center(session, wall_pixel);
    CHECK(session.validation_text() == "");
}

TEST_CASE("the faces of a world are forgotten when it is left, and faces described for it later are ignored") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(session.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    SceneStream hidden = tunnel_to_the_wall(closed_faces);
    hidden.publish_section(wall_section, wall_mesh());
    REQUIRE(session.submit(world_frame(hidden)) == MINEMOD_RESULT_OK);
    SceneStream next_world;
    next_world.change_world()
        .connect_section_faces(camera_section, open_faces)
        .connect_section_faces(section_before_the_camera, closed_faces)
        .publish_section(wall_section_of_the_next_world, wall_mesh());

    REQUIRE(session.submit(world_frame(next_world)) == MINEMOD_RESULT_OK);

    check_center(session, wall_pixel);
    CHECK(session.validation_text() == "");
}

TEST_CASE("connections past the six faces of a section are malformed") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = SceneStream().connect_section_faces(camera_section, pair_past_the_faces).build();

    CHECK(session.submit(stream) == MINEMOD_RESULT_MALFORMED_STREAM);
}
