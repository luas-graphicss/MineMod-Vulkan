#include <array>
#include <cstdint>
#include <vector>

#include <doctest.h>

#include "minemod/abi/scene_commands.h"
#include "minemod/scene/scene_session.h"
#include "session/support/provider_stand_in.hpp"
#include "session/support/running_session.hpp"
#include "session/support/scene_shapes.hpp"
#include "session/support/scene_stream.hpp"
#include "session/support/shared_session.hpp"
#include "session/support/terrain_frames.hpp"
#include "support/terrain_quads.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/terrain_layer.hpp"

namespace {

using namespace minemod::scene::test_support;
namespace renderer = minemod::renderer;
using renderer::test_support::QuadPlacement;

constexpr VkExtent2D frame_extent{64, 64};
constexpr Color red{255, 0, 0, 255};
constexpr Color green{0, 255, 0, 255};
constexpr Color white{255, 255, 255, 255};
constexpr Color half_blue{0, 0, 255, 128};
constexpr std::array<float, 4> provider_green{0.0f, 1.0f, 0.0f, 1.0f};
constexpr VkRect2D whole_frame{{0, 0}, {64, 64}};
constexpr VkRect2D left_half{{0, 0}, {32, 64}};
constexpr VkRect2D upper_left_badge{{0, 0}, {16, 16}};
constexpr VkRect2D lower_right_badge{{48, 48}, {16, 16}};
constexpr float near_depth = 0.25f;
constexpr float far_depth = 0.75f;
constexpr float four_pixels = 4.0f;
constexpr std::uint32_t once = 1;
constexpr std::uint32_t unknown_synchronization = 2;
constexpr SectionKey wall_section{0, 0, -3, 0};
constexpr SectionKey water_section{0, 0, -2, 0};
constexpr QuadPlacement wall_in_the_middle{5.4f, 0.0f, 10.6f, 16.0f, 8.0f};
constexpr QuadPlacement water_before_the_wall{6.0f, 4.0f, 12.0f, 12.0f, 8.0f};

SceneStream open_frame() {
    ViewState view;
    view.projection = pixel_projection(frame_extent);
    SceneStream stream;
    stream.begin_frame(terrain_backdrop, 0).set_view(view);
    return stream;
}

BatchState tested_against_depth() {
    BatchState batch;
    batch.depth = MINEMOD_DEPTH_TEST | MINEMOD_DEPTH_WRITE;
    batch.depth_compare = MINEMOD_DEPTH_COMPARE_LESS_OR_EQUAL;
    return batch;
}

renderer::SectionMesh quad_mesh(renderer::TerrainLayer layer, const QuadPlacement& placement, const Color& color) {
    renderer::SectionMesh mesh{};
    renderer::test_support::append_quad(mesh, layer, placement, {color, {0.5f, 0.5f}, {240, 240}});
    return mesh;
}

minemod_result hand_over(RunningSession& session) {
    return minemod_scene_session_release_to_provider(session.get(), MINEMOD_HANDOFF_BLOCKING);
}

minemod_result take_back(RunningSession& session) {
    return minemod_scene_session_reclaim_for_renderer(session.get());
}

DrawnFrame draw_in_one_part(
    RunningSession& session, const std::vector<std::uint8_t>& beginning, const std::vector<std::uint8_t>& rest) {
    REQUIRE(session.submit(beginning) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(rest) == MINEMOD_RESULT_OK);
    return session.read_frame(frame_extent.width, frame_extent.height);
}

DrawnFrame draw_around_a_handoff(
    RunningSession& session, const std::vector<std::uint8_t>& beginning, const std::vector<std::uint8_t>& rest) {
    REQUIRE(session.submit(beginning) == MINEMOD_RESULT_OK);
    REQUIRE(hand_over(session) == MINEMOD_RESULT_OK);
    REQUIRE(take_back(session) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(rest) == MINEMOD_RESULT_OK);
    return session.read_frame(frame_extent.width, frame_extent.height);
}

void check_pixel(const DrawnFrame& frame, std::uint32_t x, std::uint32_t y, const Rgb& expected) {
    CHECK_MESSAGE(frame.shows(x, y, expected), frame.describe(x, y));
}

}

TEST_CASE("a provider draws on the frame between two parts and the renderer continues over it") {
    RunningSession session(shared_config(frame_extent));
    ProviderStandIn provider(
        sharing_of(session).attachments[MINEMOD_SHARED_ATTACHMENT_COLOR],
        exported_memory(session, MINEMOD_SHARED_ATTACHMENT_COLOR));
    const std::vector<std::uint8_t> beginning =
        open_frame().draw_batch(BatchState{}, colored_rectangle(upper_left_badge, white)).build();
    const std::vector<std::uint8_t> rest =
        SceneStream().draw_batch(BatchState{}, colored_rectangle(lower_right_badge, red)).end_frame().build();

    REQUIRE(session.submit(beginning) == MINEMOD_RESULT_OK);
    REQUIRE(hand_over(session) == MINEMOD_RESULT_OK);
    const DrawnFrame handed_over = provider.fill_after_reading(provider_green);
    REQUIRE(take_back(session) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(rest) == MINEMOD_RESULT_OK);

    check_pixel(handed_over, 8, 8, {255, 255, 255});
    check_pixel(handed_over, 32, 32, terrain_backdrop_pixel);
    const DrawnFrame finished = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(finished, 8, 8, {0, 255, 0});
    check_pixel(finished, 32, 32, {0, 255, 0});
    check_pixel(finished, 56, 56, {255, 0, 0});
    CHECK(session.validation_text() == "");
}

TEST_CASE("what is drawn after a handoff is tested against the depth written before it") {
    RunningSession session(shared_config(frame_extent));
    const std::vector<std::uint8_t> beginning =
        open_frame()
            .draw_batch(tested_against_depth(), colored_rectangle_at(upper_left_badge, near_depth, red))
            .build();
    const std::vector<std::uint8_t> rest =
        SceneStream()
            .draw_batch(tested_against_depth(), colored_rectangle_at(whole_frame, far_depth, green))
            .end_frame()
            .build();

    const DrawnFrame frame = draw_around_a_handoff(session, beginning, rest);

    check_pixel(frame, 8, 8, {255, 0, 0});
    check_pixel(frame, 40, 40, {0, 255, 0});
    CHECK(session.validation_text() == "");
}

TEST_CASE("terrain drawn before a handoff is not drawn again by the rest of the frame") {
    RunningSession whole(shared_config(frame_extent));
    RunningSession split(shared_config(frame_extent));
    REQUIRE(whole.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    REQUIRE(split.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    const std::vector<std::uint8_t> beginning =
        SceneStream()
            .publish_section(wall_section, quad_mesh(renderer::TerrainLayer::solid, wall_in_the_middle, red))
            .publish_section(
                water_section, quad_mesh(renderer::TerrainLayer::translucent, water_before_the_wall, half_blue))
            .begin_frame(terrain_backdrop, MINEMOD_FRAME_DRAWS_WORLD)
            .set_terrain(terrain_seen_from())
            .build();
    const std::vector<std::uint8_t> rest = SceneStream().end_frame().build();

    const DrawnFrame expected = draw_in_one_part(whole, beginning, rest);
    const DrawnFrame drawn = draw_around_a_handoff(split, beginning, rest);

    check_pixel(drawn, 32, 32, {127, 0, 128});
    check_pixel(drawn, 4, 32, terrain_backdrop_pixel);
    CHECK(drawn == expected);
    CHECK(split.validation_text() == "");
}

TEST_CASE("a frame handed over before its terrain is described draws the terrain when it is") {
    RunningSession session(shared_config(frame_extent));
    REQUIRE(session.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    const std::vector<std::uint8_t> beginning =
        SceneStream().begin_frame(terrain_backdrop, MINEMOD_FRAME_DRAWS_WORLD).build();
    const std::vector<std::uint8_t> rest =
        SceneStream()
            .publish_section(wall_section, quad_mesh(renderer::TerrainLayer::solid, wall_in_the_middle, red))
            .set_terrain(terrain_seen_from())
            .end_frame()
            .build();

    const DrawnFrame frame = draw_around_a_handoff(session, beginning, rest);

    check_pixel(frame, 32, 32, {255, 0, 0});
    check_pixel(frame, 4, 32, terrain_backdrop_pixel);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a backdrop blurred before a handoff is not blurred again by the rest of the frame") {
    RunningSession whole(shared_config(frame_extent));
    RunningSession split(shared_config(frame_extent));
    const std::vector<std::uint8_t> beginning =
        open_frame()
            .draw_batch(BatchState{}, colored_rectangle(left_half, white))
            .blur_frame(four_pixels, once)
            .build();
    const std::vector<std::uint8_t> rest =
        SceneStream().draw_batch(BatchState{}, colored_rectangle(lower_right_badge, red)).end_frame().build();

    const DrawnFrame expected = draw_in_one_part(whole, beginning, rest);
    const DrawnFrame drawn = draw_around_a_handoff(split, beginning, rest);

    check_pixel(drawn, 8, 32, {255, 255, 255});
    check_pixel(drawn, 56, 56, {255, 0, 0});
    CHECK(drawn == expected);
    CHECK(split.validation_text() == "");
}

TEST_CASE("a frame handed over twice keeps what was drawn before each handoff") {
    RunningSession session(shared_config(frame_extent));
    REQUIRE(
        session.submit(open_frame().draw_batch(BatchState{}, colored_rectangle(upper_left_badge, white)).build())
        == MINEMOD_RESULT_OK);

    REQUIRE(hand_over(session) == MINEMOD_RESULT_OK);
    REQUIRE(take_back(session) == MINEMOD_RESULT_OK);
    REQUIRE(hand_over(session) == MINEMOD_RESULT_OK);
    REQUIRE(take_back(session) == MINEMOD_RESULT_OK);
    REQUIRE(
        session.submit(
            SceneStream().draw_batch(BatchState{}, colored_rectangle(lower_right_badge, red)).end_frame().build())
        == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 8, 8, {255, 255, 255});
    check_pixel(frame, 56, 56, {255, 0, 0});
    check_pixel(frame, 32, 32, terrain_backdrop_pixel);
    CHECK(session.counters().batches_drawn == 2);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a frame is handed over only while it is open and taken back only once") {
    RunningSession session(shared_config(frame_extent));
    std::vector<std::uint8_t> pixels(std::size_t{frame_extent.width} * frame_extent.height * 4);
    REQUIRE(session.submit(open_frame().end_frame().build()) == MINEMOD_RESULT_OK);

    CHECK(hand_over(session) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(take_back(session) == MINEMOD_RESULT_INVALID_ARGUMENT);
    REQUIRE(session.submit(open_frame().build()) == MINEMOD_RESULT_OK);
    CHECK(
        minemod_scene_session_release_to_provider(session.get(), unknown_synchronization)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
    REQUIRE(hand_over(session) == MINEMOD_RESULT_OK);
    CHECK(hand_over(session) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(session.submit(SceneStream().end_frame().build()) == MINEMOD_RESULT_INVALID_ARGUMENT);
    REQUIRE(take_back(session) == MINEMOD_RESULT_OK);
    CHECK(take_back(session) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(
        minemod_scene_session_read_frame(session.get(), pixels.data(), pixels.size())
        == MINEMOD_RESULT_INVALID_ARGUMENT);

    REQUIRE(
        session.submit(
            SceneStream().draw_batch(BatchState{}, colored_rectangle(upper_left_badge, white)).end_frame().build())
        == MINEMOD_RESULT_OK);
    check_pixel(session.read_frame(frame_extent.width, frame_extent.height), 8, 8, {255, 255, 255});
    CHECK(session.validation_text() == "");
}

TEST_CASE("a session created for itself hands its frames to nobody") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(session.submit(open_frame().build()) == MINEMOD_RESULT_OK);

    CHECK(hand_over(session) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(take_back(session) == MINEMOD_RESULT_INVALID_ARGUMENT);

    CHECK(session.submit(SceneStream().end_frame().build()) == MINEMOD_RESULT_OK);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a handoff synchronized by semaphores is refused until they were exported") {
    RunningSession session(shared_config(frame_extent));
    const bool shares_semaphores = (sharing_of(session).flags & MINEMOD_SCENE_SHARING_SEMAPHORES) != 0;
    REQUIRE(session.submit(open_frame().build()) == MINEMOD_RESULT_OK);

    const minemod_result released =
        minemod_scene_session_release_to_provider(session.get(), MINEMOD_HANDOFF_SEMAPHORES);

    CHECK(released == (shares_semaphores ? MINEMOD_RESULT_INVALID_ARGUMENT : MINEMOD_RESULT_UNSUPPORTED_CAPABILITY));
    CHECK(take_back(session) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(session.submit(SceneStream().end_frame().build()) == MINEMOD_RESULT_OK);
    CHECK(session.validation_text() == "");
}
