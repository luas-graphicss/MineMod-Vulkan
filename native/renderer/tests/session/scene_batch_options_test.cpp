#include <array>
#include <cstddef>
#include <cstdint>
#include <vector>

#include <doctest.h>

#include "minemod/abi/scene_commands.h"
#include "session/support/running_session.hpp"
#include "session/support/scene_shapes.hpp"
#include "session/support/scene_stream.hpp"

namespace {

using namespace minemod::scene::test_support;

constexpr VkExtent2D frame_extent{64, 64};
constexpr VkExtent2D two_texels{2, 1};
constexpr std::array<float, 4> dark_gray{32.0f / 255.0f, 32.0f / 255.0f, 32.0f / 255.0f, 1.0f};
constexpr Rgb background{32, 32, 32};
constexpr Color red{255, 0, 0, 255};
constexpr Color green{0, 255, 0, 255};
constexpr Color blue{0, 0, 255, 255};
constexpr Color white{255, 255, 255, 255};
constexpr VkRect2D whole_frame{{0, 0}, {64, 64}};
constexpr VkRect2D upper_left_badge{{0, 0}, {16, 16}};
constexpr VkRect2D lower_right_badge{{48, 48}, {16, 16}};
constexpr ClipState upper_left_quarter{0, 32, 32, 32};
constexpr ClipState reaching_past_the_upper_left_corner{-16, 48, 48, 64};
constexpr std::array<float, 3> right_half_of_u{0.5f, 0.0f, 0.5f};
constexpr std::array<float, 3> unchanged_u{1.0f, 0.0f, 0.0f};
constexpr std::array<float, 3> unchanged_v{0.0f, 1.0f, 0.0f};
constexpr float near_depth = 0.25f;
constexpr float far_depth = 0.75f;
constexpr std::ptrdiff_t colored_vertex_bytes = 16;
constexpr std::ptrdiff_t corners_per_triangle = 3;

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

std::vector<std::uint8_t> joined(std::vector<std::uint8_t> first, const std::vector<std::uint8_t>& second) {
    first.insert(first.end(), second.begin(), second.end());
    return first;
}

std::vector<std::uint8_t> frame_drawing(std::uint32_t order, const std::vector<std::uint8_t>& quads) {
    return open_frame().set_quad_order(order).draw_batch(BatchState{}, quads).end_frame().build();
}

void check_pixel(const DrawnFrame& frame, std::uint32_t x, std::uint32_t y, const Rgb& expected) {
    CHECK_MESSAGE(frame.shows(x, y, expected), frame.describe(x, y));
}

}

TEST_CASE("a clip keeps the batches that follow inside its rectangle until it is cleared") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = open_frame()
                            .set_clip(upper_left_quarter)
                            .draw_batch(BatchState{}, colored_rectangle(whole_frame, red))
                            .clear_clip()
                            .draw_batch(BatchState{}, colored_rectangle(lower_right_badge, green))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 8, 8, {255, 0, 0});
    check_pixel(frame, 40, 8, background);
    check_pixel(frame, 8, 40, background);
    check_pixel(frame, 56, 56, {0, 255, 0});
    CHECK(session.validation_text() == "");
}

TEST_CASE("a clip reaching past the frame is cut at its edges") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = open_frame()
                            .set_clip(reaching_past_the_upper_left_corner)
                            .draw_batch(BatchState{}, colored_rectangle(whole_frame, red))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 24, 8, {255, 0, 0});
    check_pixel(frame, 40, 8, background);
    check_pixel(frame, 24, 24, background);
    CHECK(session.validation_text() == "");
}

TEST_CASE("the options of a frame are forgotten when it ends") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto clipped = open_frame()
                             .set_clip(upper_left_quarter)
                             .draw_batch(BatchState{}, colored_rectangle(whole_frame, red))
                             .end_frame()
                             .build();
    const auto unclipped =
        open_frame().draw_batch(BatchState{}, colored_rectangle(whole_frame, green)).end_frame().build();

    REQUIRE(session.submit(clipped) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(unclipped) == MINEMOD_RESULT_OK);

    check_pixel(session.read_frame(frame_extent.width, frame_extent.height), 48, 48, {0, 255, 0});
    CHECK(session.validation_text() == "");
}

TEST_CASE("a texture transform moves the texels that the following batches sample") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const minemod_handle texture = texture_handle(1);
    BatchState textured;
    textured.elements = MINEMOD_VERTEX_POSITION | MINEMOD_VERTEX_COLOR | MINEMOD_VERTEX_TEXTURE;
    textured.shading = MINEMOD_SHADING_TEXTURE;
    textured.texture = texture;
    SceneStream stream;
    stream.define_texture(texture, two_texels.width, two_texels.height, 1, 0)
        .write_texture(texture, {0, 0, 0, 1, 1}, red)
        .write_texture(texture, {0, 1, 0, 1, 1}, blue)
        .begin_frame(dark_gray, 0)
        .set_view(canvas_view())
        .set_clip(upper_left_quarter)
        .set_texture_transform(right_half_of_u, unchanged_v)
        .draw_batch(textured, textured_rectangle(whole_frame, white))
        .clear_clip()
        .set_texture_transform(unchanged_u, unchanged_v)
        .draw_batch(textured, textured_rectangle(lower_right_badge, white))
        .end_frame();

    REQUIRE(session.submit(stream.build()) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 4, 16, {0, 0, 255});
    check_pixel(frame, 28, 16, {0, 0, 255});
    check_pixel(frame, 52, 56, {255, 0, 0});
    check_pixel(frame, 60, 56, {0, 0, 255});
    CHECK(session.validation_text() == "");
}

TEST_CASE("quads ordered farthest first end with the quad nearest to the origin") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const std::vector<std::uint8_t> badge_then_frame =
        joined(colored_rectangle(upper_left_badge, green), colored_rectangle(whole_frame, red));

    REQUIRE(session.submit(frame_drawing(MINEMOD_QUAD_ORDER_SUBMITTED, badge_then_frame)) == MINEMOD_RESULT_OK);
    check_pixel(session.read_frame(frame_extent.width, frame_extent.height), 8, 8, {255, 0, 0});

    REQUIRE(session.submit(frame_drawing(MINEMOD_QUAD_ORDER_FARTHEST_FIRST, badge_then_frame)) == MINEMOD_RESULT_OK);
    check_pixel(session.read_frame(frame_extent.width, frame_extent.height), 8, 8, {0, 255, 0});
    CHECK(session.validation_text() == "");
}

TEST_CASE("quads ordered by rising depth end with the deepest quad") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const std::vector<std::uint8_t> deep_then_shallow = joined(
        colored_rectangle_at(whole_frame, far_depth, green), colored_rectangle_at(whole_frame, near_depth, red));

    REQUIRE(session.submit(frame_drawing(MINEMOD_QUAD_ORDER_SUBMITTED, deep_then_shallow)) == MINEMOD_RESULT_OK);
    check_pixel(session.read_frame(frame_extent.width, frame_extent.height), 32, 32, {255, 0, 0});

    REQUIRE(session.submit(frame_drawing(MINEMOD_QUAD_ORDER_RISING_DEPTH, deep_then_shallow)) == MINEMOD_RESULT_OK);
    check_pixel(session.read_frame(frame_extent.width, frame_extent.height), 32, 32, {0, 255, 0});
    CHECK(session.validation_text() == "");
}

TEST_CASE("a quad order leaves batches that are not quads as they were submitted") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const std::vector<std::uint8_t> rectangle = colored_rectangle(whole_frame, red);
    const std::vector<std::uint8_t> lower_left_triangle(
        rectangle.begin(), rectangle.begin() + corners_per_triangle * colored_vertex_bytes);
    BatchState triangles;
    triangles.mode = MINEMOD_PRIMITIVE_TRIANGLES;
    const auto stream = open_frame()
                            .set_quad_order(MINEMOD_QUAD_ORDER_FARTHEST_FIRST)
                            .draw_batch(triangles, lower_left_triangle)
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 8, 56, {255, 0, 0});
    check_pixel(frame, 56, 8, background);
    CHECK(session.validation_text() == "");
}

TEST_CASE("inversion and darkening blending reach the batches of a frame") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    BatchState inverting;
    inverting.blending = MINEMOD_BLENDING_INVERSION;
    BatchState darkening;
    darkening.blending = MINEMOD_BLENDING_DARKENING;
    const auto stream = open_frame()
                            .draw_batch(BatchState{}, colored_rectangle(whole_frame, white))
                            .draw_batch(inverting, colored_rectangle(upper_left_badge, white))
                            .draw_batch(darkening, colored_rectangle(lower_right_badge, red))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 8, 8, {0, 0, 0});
    check_pixel(frame, 56, 56, {0, 255, 255});
    check_pixel(frame, 32, 32, {255, 255, 255});
    CHECK(session.validation_text() == "");
}

TEST_CASE("batch options are refused outside a frame") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    SceneStream stream;

    SUBCASE("a clip") {
        stream.set_clip(upper_left_quarter);
    }
    SUBCASE("a cleared clip") {
        stream.clear_clip();
    }
    SUBCASE("a texture transform") {
        stream.set_texture_transform(unchanged_u, unchanged_v);
    }
    SUBCASE("a quad order") {
        stream.set_quad_order(MINEMOD_QUAD_ORDER_FARTHEST_FIRST);
    }

    CHECK(session.submit(stream.build()) == MINEMOD_RESULT_INVALID_ARGUMENT);
}

TEST_CASE("a clip of negative size and an unknown quad order are malformed") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    SceneStream stream;
    stream.begin_frame(dark_gray, 0);

    SUBCASE("a clip narrower than nothing") {
        stream.set_clip({0, 0, -1, 8});
    }
    SUBCASE("a clip shorter than nothing") {
        stream.set_clip({0, 0, 8, -1});
    }
    SUBCASE("an order past the known ones") {
        stream.set_quad_order(MINEMOD_QUAD_ORDER_RISING_DEPTH + 1);
    }

    CHECK(session.submit(stream.build()) == MINEMOD_RESULT_MALFORMED_STREAM);
}
