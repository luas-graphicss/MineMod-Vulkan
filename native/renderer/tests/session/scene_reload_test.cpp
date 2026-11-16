#include <stdint.h>

#include <array>
#include <cstdint>
#include <string>
#include <vector>

#include <doctest.h>

#include "minemod/abi/scene_commands.h"
#include "scene_frag.h"
#include "scene_vert.h"
#include "session/support/running_session.hpp"
#include "session/support/scene_shapes.hpp"
#include "session/support/scene_stream.hpp"
#include "session/support/session_reload.hpp"

namespace {

using namespace minemod::scene::test_support;

constexpr VkExtent2D frame_extent{64, 64};
constexpr std::array<float, 4> dark_gray{32.0f / 255.0f, 32.0f / 255.0f, 32.0f / 255.0f, 1.0f};
constexpr Rgb background{32, 32, 32};
constexpr Color red{255, 0, 0, 255};
constexpr VkRect2D upper_left_badge{{0, 0}, {16, 16}};
constexpr std::uint32_t entity_program = 7;
constexpr std::array<std::uint32_t, 3> three_words{1, 2, 3};

ViewState canvas_view() {
    ViewState state;
    state.projection = pixel_projection(frame_extent);
    return state;
}

std::vector<std::uint8_t> badge_frame(std::uint32_t program) {
    BatchState batch;
    batch.program = program;
    return SceneStream()
        .begin_frame(dark_gray, 0)
        .set_view(canvas_view())
        .draw_batch(batch, colored_rectangle(upper_left_badge, red))
        .end_frame()
        .build();
}

minemod_result stage_scene_shaders(RunningSession& session, std::uint32_t program) {
    return stage_program(session.get(), program, scene_vert, scene_frag);
}

void check_badge(RunningSession& session) {
    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    CHECK_MESSAGE(frame.shows(8, 8, {255, 0, 0}), frame.describe(8, 8));
    CHECK_MESSAGE(frame.shows(40, 40, background), frame.describe(40, 40));
}

}

TEST_CASE("a committed program draws the batches that name it and advances the resource generation") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(minemod_scene_session_begin_reload(session.get()) == MINEMOD_RESULT_OK);
    REQUIRE(stage_scene_shaders(session, entity_program) == MINEMOD_RESULT_OK);

    REQUIRE(minemod_scene_session_commit_reload(session.get()) == MINEMOD_RESULT_OK);

    REQUIRE(session.submit(badge_frame(entity_program)) == MINEMOD_RESULT_OK);
    check_badge(session);
    CHECK(metrics_of(session.get()).resource_generation == 1);
    CHECK(reload_text(session.get()) == "");
    CHECK(session.validation_text() == "");
}

TEST_CASE("a batch that names a program nobody published is drawn by the built-in shaders") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));

    REQUIRE(session.submit(badge_frame(entity_program)) == MINEMOD_RESULT_OK);

    check_badge(session);
    CHECK(session.validation_text() == "");
}

TEST_CASE("words that are not SPIR-V refuse the reload and keep the generation in use") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(minemod_scene_session_begin_reload(session.get()) == MINEMOD_RESULT_OK);

    CHECK(stage_program(session.get(), entity_program, three_words, scene_frag) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_scene_session_commit_reload(session.get()) == MINEMOD_RESULT_INVALID_ARGUMENT);

    CHECK(
        reload_text(session.get())
        == "program 7: core shader 'scene': the SPIR-V module is shorter than its header\n");
    CHECK(metrics_of(session.get()).resource_generation == 0);
    REQUIRE(session.submit(badge_frame(entity_program)) == MINEMOD_RESULT_OK);
    check_badge(session);
    CHECK(session.validation_text() == "");
}

TEST_CASE("modules outside the contract refuse the whole reload, also the programs that were accepted") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(minemod_scene_session_begin_reload(session.get()) == MINEMOD_RESULT_OK);
    REQUIRE(stage_scene_shaders(session, 3) == MINEMOD_RESULT_OK);

    CHECK(stage_program(session.get(), entity_program, scene_frag, scene_vert) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_scene_session_commit_reload(session.get()) == MINEMOD_RESULT_INVALID_ARGUMENT);

    CHECK(
        reload_text(session.get()).starts_with(
            "program 7: core shader 'scene': the vertex stage was given a fragment shader\n"));
    CHECK(metrics_of(session.get()).resource_generation == 0);
    CHECK(minemod_scene_session_begin_reload(session.get()) == MINEMOD_RESULT_OK);
    CHECK(reload_text(session.get()) == "");
}

TEST_CASE("a reload is opened once, staged only while open and aborted without publishing") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));

    CHECK(stage_scene_shaders(session, entity_program) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_scene_session_commit_reload(session.get()) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_scene_session_abort_reload(session.get()) == MINEMOD_RESULT_INVALID_ARGUMENT);
    REQUIRE(minemod_scene_session_begin_reload(session.get()) == MINEMOD_RESULT_OK);
    CHECK(minemod_scene_session_begin_reload(session.get()) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(stage_scene_shaders(session, 0) == MINEMOD_RESULT_INVALID_ARGUMENT);
    REQUIRE(stage_scene_shaders(session, entity_program) == MINEMOD_RESULT_OK);
    CHECK(stage_scene_shaders(session, entity_program) == MINEMOD_RESULT_INVALID_ARGUMENT);

    CHECK(minemod_scene_session_abort_reload(session.get()) == MINEMOD_RESULT_OK);

    CHECK(metrics_of(session.get()).resource_generation == 0);
    CHECK(session.validation_text() == "");
}

TEST_CASE("missing modules and modules beyond the size limit are refused before they are read") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(minemod_scene_session_begin_reload(session.get()) == MINEMOD_RESULT_OK);

    CHECK(
        minemod_scene_session_stage_program(session.get(), entity_program, nullptr, 4, scene_frag, std::size(scene_frag))
        == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(
        minemod_scene_session_stage_program(
            session.get(), entity_program, scene_vert, MINEMOD_SCENE_MAX_PROGRAM_WORDS + 1, scene_frag, std::size(scene_frag))
        == MINEMOD_RESULT_LIMIT_EXCEEDED);
    CHECK(minemod_scene_session_begin_reload(nullptr) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_scene_session_read_reload_text(nullptr, nullptr, 0) == 0);
}

TEST_CASE("a reload is published between frames, never in the middle of one") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto opened = SceneStream().begin_frame(dark_gray, 0).set_view(canvas_view()).build();
    BatchState batch;
    batch.program = entity_program;
    const auto closed = SceneStream().draw_batch(batch, colored_rectangle(upper_left_badge, red)).end_frame().build();
    REQUIRE(minemod_scene_session_begin_reload(session.get()) == MINEMOD_RESULT_OK);
    REQUIRE(stage_scene_shaders(session, entity_program) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(opened) == MINEMOD_RESULT_OK);

    CHECK(minemod_scene_session_commit_reload(session.get()) == MINEMOD_RESULT_INVALID_ARGUMENT);
    REQUIRE(session.submit(closed) == MINEMOD_RESULT_OK);
    CHECK(minemod_scene_session_commit_reload(session.get()) == MINEMOD_RESULT_OK);

    check_badge(session);
    CHECK(metrics_of(session.get()).resource_generation == 1);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a replaced generation waits for the frames that drew it and is then released") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(minemod_scene_session_begin_reload(session.get()) == MINEMOD_RESULT_OK);
    REQUIRE(stage_scene_shaders(session, entity_program) == MINEMOD_RESULT_OK);
    REQUIRE(minemod_scene_session_commit_reload(session.get()) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(badge_frame(entity_program)) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(badge_frame(entity_program)) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(badge_frame(entity_program)) == MINEMOD_RESULT_OK);
    REQUIRE(metrics_of(session.get()).retired_resources == 0);

    REQUIRE(minemod_scene_session_begin_reload(session.get()) == MINEMOD_RESULT_OK);
    REQUIRE(minemod_scene_session_commit_reload(session.get()) == MINEMOD_RESULT_OK);

    CHECK(metrics_of(session.get()).retired_resources == 2);
    for (int frame = 0; frame < 3; ++frame) {
        REQUIRE(session.submit(badge_frame(entity_program)) == MINEMOD_RESULT_OK);
    }
    check_badge(session);
    CHECK(metrics_of(session.get()).retired_resources == 0);
    CHECK(metrics_of(session.get()).resource_generation == 2);
    CHECK(session.validation_text() == "");
}
