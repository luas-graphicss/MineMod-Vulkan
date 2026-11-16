#include <array>
#include <cstdint>
#include <limits>

#include <doctest.h>

#include "minemod/abi/scene_commands.h"
#include "session/support/running_session.hpp"
#include "session/support/scene_shapes.hpp"
#include "session/support/scene_stream.hpp"

namespace {

using namespace minemod::scene::test_support;

constexpr VkExtent2D frame_extent{64, 64};
constexpr std::array<float, 4> dark_gray{32.0f / 255.0f, 32.0f / 255.0f, 32.0f / 255.0f, 1.0f};
constexpr Rgb background{32, 32, 32};
constexpr Rgb pure_white{255, 255, 255};
constexpr Color red{255, 0, 0, 255};
constexpr Color green{0, 255, 0, 255};
constexpr Color white{255, 255, 255, 255};
constexpr VkRect2D whole_frame{{0, 0}, {64, 64}};
constexpr VkRect2D left_half{{0, 0}, {32, 64}};
constexpr VkRect2D upper_left_badge{{0, 0}, {16, 16}};
constexpr VkRect2D lower_right_badge{{48, 48}, {16, 16}};
constexpr float near_depth = 0.25f;
constexpr float far_depth = 0.75f;
constexpr float four_pixels = 4.0f;
constexpr std::uint32_t once = 1;

ViewState canvas_view() {
    ViewState state;
    state.projection = pixel_projection(frame_extent);
    return state;
}

SceneStream open_frame() {
    SceneStream stream;
    stream.begin_frame(dark_gray, 0).set_view(canvas_view());
    return stream;
}

BatchState tested_against_depth() {
    BatchState batch;
    batch.depth = MINEMOD_DEPTH_TEST | MINEMOD_DEPTH_WRITE;
    batch.depth_compare = MINEMOD_DEPTH_COMPARE_LESS_OR_EQUAL;
    return batch;
}

void check_pixel(const DrawnFrame& frame, std::uint32_t x, std::uint32_t y, const Rgb& expected) {
    CHECK_MESSAGE(frame.shows(x, y, expected), frame.describe(x, y));
}

}

TEST_CASE("a batch behind an earlier one stays hidden while the depth of the frame is kept") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = open_frame()
                            .draw_batch(tested_against_depth(), colored_rectangle_at(whole_frame, near_depth, red))
                            .draw_batch(tested_against_depth(), colored_rectangle_at(upper_left_badge, far_depth, green))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    check_pixel(session.read_frame(frame_extent.width, frame_extent.height), 8, 8, {255, 0, 0});
    CHECK(session.validation_text() == "");
}

TEST_CASE("a depth clear lets the batches that follow cover what was drawn nearer before it") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = open_frame()
                            .draw_batch(tested_against_depth(), colored_rectangle_at(whole_frame, near_depth, red))
                            .clear_depth()
                            .draw_batch(tested_against_depth(), colored_rectangle_at(upper_left_badge, far_depth, green))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 8, 8, {0, 255, 0});
    check_pixel(frame, 40, 40, {255, 0, 0});
    CHECK(session.validation_text() == "");
}

TEST_CASE("a depth clear is spent on the first batch that follows it") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = open_frame()
                            .clear_depth()
                            .draw_batch(tested_against_depth(), colored_rectangle_at(whole_frame, near_depth, red))
                            .draw_batch(tested_against_depth(), colored_rectangle_at(upper_left_badge, far_depth, green))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    check_pixel(session.read_frame(frame_extent.width, frame_extent.height), 8, 8, {255, 0, 0});
    CHECK(session.validation_text() == "");
}

TEST_CASE("a blur spreads the batches drawn before it and leaves the later ones sharp") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = open_frame()
                            .draw_batch(BatchState{}, colored_rectangle(left_half, white))
                            .blur_frame(four_pixels, once)
                            .draw_batch(BatchState{}, colored_rectangle(lower_right_badge, green))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 8, 8, pure_white);
    check_pixel(frame, 44, 8, background);
    CHECK_FALSE(frame.shows(30, 8, pure_white));
    CHECK_FALSE(frame.shows(30, 8, background));
    CHECK_FALSE(frame.shows(33, 8, pure_white));
    CHECK_FALSE(frame.shows(33, 8, background));
    check_pixel(frame, 56, 56, {0, 255, 0});
    check_pixel(frame, 46, 56, background);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a blur that starts a frame leaves every batch of the frame sharp") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = open_frame()
                            .blur_frame(four_pixels, once)
                            .draw_batch(BatchState{}, colored_rectangle(left_half, white))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 30, 8, pure_white);
    check_pixel(frame, 33, 8, background);
    CHECK(session.validation_text() == "");
}

TEST_CASE("repeating a blur spreads the frame farther") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto blurred = [](std::uint32_t repeats) {
        return open_frame()
            .draw_batch(BatchState{}, colored_rectangle(left_half, white))
            .blur_frame(four_pixels, repeats)
            .end_frame()
            .build();
    };

    REQUIRE(session.submit(blurred(once)) == MINEMOD_RESULT_OK);
    check_pixel(session.read_frame(frame_extent.width, frame_extent.height), 38, 8, background);
    REQUIRE(session.submit(blurred(MINEMOD_BLUR_MAX_REPEATS)) == MINEMOD_RESULT_OK);
    CHECK_FALSE(session.read_frame(frame_extent.width, frame_extent.height).shows(38, 8, background));
    CHECK(session.validation_text() == "");
}

TEST_CASE("frame effects are refused outside a frame") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    SceneStream stream;

    SUBCASE("a depth clear") {
        stream.clear_depth();
    }
    SUBCASE("a blur") {
        stream.blur_frame(four_pixels, once);
    }

    CHECK(session.submit(stream.build()) == MINEMOD_RESULT_INVALID_ARGUMENT);
}

TEST_CASE("a blur without a measured radius or a count of repeats is malformed") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    SceneStream stream;
    stream.begin_frame(dark_gray, 0);

    SUBCASE("a radius below nothing") {
        stream.blur_frame(-1.0f, once);
    }
    SUBCASE("a radius past the widest blur") {
        stream.blur_frame(MINEMOD_BLUR_MAX_RADIUS + 1.0f, once);
    }
    SUBCASE("a radius that is not a number") {
        stream.blur_frame(std::numeric_limits<float>::quiet_NaN(), once);
    }
    SUBCASE("no repeats") {
        stream.blur_frame(four_pixels, 0);
    }
    SUBCASE("more repeats than a frame takes") {
        stream.blur_frame(four_pixels, MINEMOD_BLUR_MAX_REPEATS + 1);
    }

    CHECK(session.submit(stream.build()) == MINEMOD_RESULT_MALFORMED_STREAM);
}
