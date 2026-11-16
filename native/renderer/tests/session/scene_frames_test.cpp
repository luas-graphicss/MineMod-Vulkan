#include <algorithm>
#include <array>
#include <cmath>
#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

#include <doctest.h>

#include "draw/vertex_format.hpp"
#include "math/matrix.hpp"
#include "minemod/abi/scene_commands.h"
#include "session/support/running_session.hpp"
#include "session/support/scene_shapes.hpp"
#include "session/support/scene_stream.hpp"
#include "support/scene_vertices.hpp"
#include "support/terrain_quads.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/terrain_layer.hpp"

namespace {

using namespace minemod::scene::test_support;
namespace renderer = minemod::renderer;

constexpr VkExtent2D frame_extent{64, 64};
constexpr VkExtent2D half_canvas{32, 32};
constexpr std::array<float, 4> dark_gray{32.0f / 255.0f, 32.0f / 255.0f, 32.0f / 255.0f, 1.0f};
constexpr Rgb background{32, 32, 32};
constexpr Color red{255, 0, 0, 255};
constexpr Color green{0, 255, 0, 255};
constexpr Color blue{0, 0, 255, 255};
constexpr Color white{255, 255, 255, 255};
constexpr VkRect2D upper_left_badge{{0, 0}, {16, 16}};
constexpr VkRect2D lower_right_quarter{{16, 16}, {16, 16}};
constexpr VkRect2D whole_frame{{0, 0}, {64, 64}};
constexpr VkExtent2D two_texels{2, 1};
constexpr VkExtent2D one_texel{1, 1};
constexpr VkExtent2D light_table{16, 16};
constexpr float wall_distance = 40.0f;
constexpr float wall_reach = 8.0f;
constexpr SectionKey wall_section{0, 0, -3, 0};
constexpr renderer::test_support::QuadPlacement wall_right_of_first_strip{4.0f, 0.0f, 16.0f, 16.0f, 8.0f};
constexpr renderer::test_support::QuadPlacement entity_in_third_strip{0.0f, -6.0f, 3.0f, 6.0f, -30.0f};
constexpr renderer::VertexFormat colored_format{renderer::VertexElement::position, renderer::VertexElement::color};

ViewState canvas_view(std::uint32_t view, VkExtent2D canvas) {
    ViewState state;
    state.view = view;
    state.projection = pixel_projection(canvas);
    return state;
}

std::array<float, 16> world_projection() {
    return renderer::perspective({2.0f * std::atan(wall_reach / wall_distance), 1.0f, 0.5f, 100.0f}).columns;
}

renderer::SectionMesh wall_mesh() {
    renderer::SectionMesh mesh{};
    renderer::test_support::append_quad(
        mesh, renderer::TerrainLayer::solid, wall_right_of_first_strip, {red, {0.5f, 0.5f}, {240, 240}});
    return mesh;
}

std::vector<std::uint8_t> entity_vertices() {
    renderer::test_support::SceneVertex surface;
    surface.color = green;
    const std::vector<std::byte> bytes = renderer::test_support::encode_vertices(
        colored_format, renderer::test_support::quad_corners(entity_in_third_strip, surface));
    std::vector<std::uint8_t> vertices(bytes.size());
    std::ranges::transform(bytes, vertices.begin(), [](std::byte byte) { return std::to_integer<std::uint8_t>(byte); });
    return vertices;
}

BatchState textured_with(minemod_handle texture) {
    BatchState batch;
    batch.elements = MINEMOD_VERTEX_POSITION | MINEMOD_VERTEX_COLOR | MINEMOD_VERTEX_TEXTURE;
    batch.shading = MINEMOD_SHADING_TEXTURE;
    batch.texture = texture;
    return batch;
}

void check_pixel(const DrawnFrame& frame, std::uint32_t x, std::uint32_t y, const Rgb& expected) {
    CHECK_MESSAGE(frame.shows(x, y, expected), frame.describe(x, y));
}

}

TEST_CASE("an interface frame clears the target and draws its batches") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = SceneStream()
                            .begin_frame(dark_gray, 0)
                            .set_view(canvas_view(0, frame_extent))
                            .draw_batch(BatchState{}, colored_rectangle(upper_left_badge, red))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 8, 8, {255, 0, 0});
    check_pixel(frame, 40, 40, background);
    CHECK(session.counters().frames_drawn == 1);
    CHECK(session.counters().batches_drawn == 1);
    CHECK(session.validation_text() == "");
}

TEST_CASE("batches of one stage are drawn with the view each of them names") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    BatchState on_half_canvas;
    on_half_canvas.view = 1;
    const auto stream = SceneStream()
                            .begin_frame(dark_gray, 0)
                            .set_view(canvas_view(0, frame_extent))
                            .set_view(canvas_view(1, half_canvas))
                            .draw_batch(BatchState{}, colored_rectangle(upper_left_badge, red))
                            .draw_batch(on_half_canvas, colored_rectangle(lower_right_quarter, green))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const DrawnFrame frame = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(frame, 8, 8, {255, 0, 0});
    check_pixel(frame, 48, 48, {0, 255, 0});
    check_pixel(frame, 24, 24, background);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a texture written in regions is sampled, and a later write keeps the texels around it") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const minemod_handle texture = texture_handle(1);
    const auto draw_texture = [&](SceneStream& stream) {
        return stream.begin_frame(dark_gray, 0)
            .set_view(canvas_view(0, frame_extent))
            .draw_batch(textured_with(texture), textured_rectangle(whole_frame, white))
            .end_frame()
            .build();
    };
    SceneStream first;
    first.define_texture(texture, two_texels.width, two_texels.height, 1, 0)
        .write_texture(texture, {0, 0, 0, 1, 1}, red)
        .write_texture(texture, {0, 1, 0, 1, 1}, blue);

    REQUIRE(session.submit(draw_texture(first)) == MINEMOD_RESULT_OK);
    const DrawnFrame written = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(written, 16, 32, {255, 0, 0});
    check_pixel(written, 48, 32, {0, 0, 255});

    SceneStream second;
    second.write_texture(texture, {0, 0, 0, 1, 1}, green);

    REQUIRE(session.submit(draw_texture(second)) == MINEMOD_RESULT_OK);
    const DrawnFrame rewritten = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(rewritten, 16, 32, {0, 255, 0});
    check_pixel(rewritten, 48, 32, {0, 0, 255});
    CHECK(session.validation_text() == "");
}

TEST_CASE("a world frame draws published terrain, batches in front of it and the interface over both") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const minemod_handle atlas = texture_handle(1);
    const minemod_handle lightmap = texture_handle(2);
    TerrainState terrain;
    terrain.view_projection = world_projection();
    terrain.camera = {8.0, 8.0, 0.0};
    terrain.atlas = atlas;
    terrain.lightmap = lightmap;
    ViewState world_view;
    world_view.projection = world_projection();
    world_view.lightmap = lightmap;
    BatchState entity;
    entity.slot = MINEMOD_BATCH_SOLID;
    entity.depth = MINEMOD_DEPTH_TEST | MINEMOD_DEPTH_WRITE;
    entity.depth_compare = MINEMOD_DEPTH_COMPARE_LESS_OR_EQUAL;
    entity.culling = MINEMOD_CULLING_BACK_FACES;
    BatchState badge;
    badge.view = 1;
    const auto draw_world = [&](SceneStream& stream) {
        return stream.begin_frame(dark_gray, MINEMOD_FRAME_DRAWS_WORLD)
            .set_terrain(terrain)
            .set_view(world_view)
            .set_view(canvas_view(1, frame_extent))
            .draw_batch(entity, entity_vertices())
            .draw_batch(badge, colored_rectangle(upper_left_badge, white))
            .end_frame()
            .build();
    };
    SceneStream loaded;
    loaded.define_texture(atlas, one_texel.width, one_texel.height, 1, 0)
        .write_texture(atlas, {0, 0, 0, 1, 1}, white)
        .define_texture(lightmap, light_table.width, light_table.height, 1, MINEMOD_SAMPLING_LINEAR)
        .write_texture(lightmap, {0, 0, 0, light_table.width, light_table.height}, solid_texels(light_table, white))
        .publish_section(wall_section, wall_mesh());

    REQUIRE(session.submit(draw_world(loaded)) == MINEMOD_RESULT_OK);
    const DrawnFrame with_wall = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(with_wall, 24, 24, {255, 0, 0});
    check_pixel(with_wall, 40, 24, {0, 255, 0});
    check_pixel(with_wall, 8, 40, background);
    check_pixel(with_wall, 8, 8, {255, 255, 255});
    CHECK(session.counters().live_sections == 1);

    SceneStream unloaded;
    unloaded.unload_section(wall_section);

    REQUIRE(session.submit(draw_world(unloaded)) == MINEMOD_RESULT_OK);
    const DrawnFrame without_wall = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(without_wall, 24, 24, background);
    check_pixel(without_wall, 40, 24, {0, 255, 0});
    CHECK(session.counters().live_sections == 0);
    CHECK(session.counters().frames_drawn == 2);
    CHECK(session.validation_text() == "");
}
