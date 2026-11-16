#include <array>
#include <cmath>
#include <cstdint>
#include <string>
#include <vector>

#include <doctest.h>

#include "math/matrix.hpp"
#include "minemod/abi/scene_commands.h"
#include "session/support/running_session.hpp"
#include "session/support/scene_shapes.hpp"
#include "session/support/scene_stream.hpp"
#include "session/support/session_reload.hpp"
#include "support/terrain_quads.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/terrain_layer.hpp"

namespace {

using namespace minemod::scene::test_support;
namespace renderer = minemod::renderer;

constexpr VkExtent2D frame_extent{64, 64};
constexpr std::array<float, 4> dark_gray{32.0f / 255.0f, 32.0f / 255.0f, 32.0f / 255.0f, 1.0f};
constexpr Color red{255, 0, 0, 255};
constexpr Color white{255, 255, 255, 255};
constexpr VkRect2D upper_left_badge{{0, 0}, {16, 16}};
constexpr VkExtent2D one_texel{1, 1};
constexpr VkExtent2D light_table{16, 16};
constexpr SectionKey wall_section{0, 0, -3, 0};
constexpr renderer::test_support::QuadPlacement wall{4.0f, 0.0f, 16.0f, 16.0f, 8.0f};

ViewState canvas_view(std::uint32_t view) {
    ViewState state;
    state.view = view;
    state.projection = pixel_projection(frame_extent);
    return state;
}

std::vector<std::uint8_t> badge_frame() {
    return SceneStream()
        .begin_frame(dark_gray, 0)
        .set_view(canvas_view(0))
        .draw_batch(BatchState{}, colored_rectangle(upper_left_badge, red))
        .end_frame()
        .build();
}

std::array<float, 16> world_projection() {
    return renderer::perspective({2.0f * std::atan(0.2f), 1.0f, 0.5f, 100.0f}).columns;
}

renderer::SectionMesh wall_mesh() {
    renderer::SectionMesh mesh{};
    renderer::test_support::append_quad(mesh, renderer::TerrainLayer::solid, wall, {red, {0.5f, 0.5f}, {240, 240}});
    return mesh;
}

std::string line_of(const std::string& report, const std::string& start) {
    const std::size_t found = report.find(start);
    if (found == std::string::npos) {
        return {};
    }
    return report.substr(found, report.find('\n', found) - found);
}

}

TEST_CASE("a session that drew nothing reports no frames, its limits and the first generation") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));

    const std::string report = metrics_text(session.get());

    CHECK(report.starts_with("frames: 0\n"));
    CHECK(line_of(report, "limit upload staging: ") == "limit upload staging: 1048576 byte(s) per frame");
    CHECK(line_of(report, "limit textures: ") == "limit textures: 64");
    CHECK(line_of(report, "limit sections: ") == "limit sections: 64");
    CHECK(line_of(report, "retired resources: ") == "retired resources: 0 waiting");
    CHECK(line_of(report, "resource generation: ") == "resource generation: 0");
    CHECK(metrics_of(session.get()).frames_measured == 0);
}

TEST_CASE("every drawn frame is measured and the draws of a pass add up") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(session.submit(badge_frame()) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(badge_frame()) == MINEMOD_RESULT_OK);

    const minemod_scene_metrics metrics = metrics_of(session.get());
    const std::string report = metrics_text(session.get());

    CHECK(metrics.frames_measured == 2);
    CHECK(metrics.worst_frame_nanoseconds >= metrics.mean_frame_nanoseconds);
    CHECK(metrics.mean_frame_nanoseconds > 0);
    CHECK(metrics.peak_pipelines == 1);
    CHECK(report.starts_with("frames: 2\n"));
    CHECK(line_of(report, "pass interface: ").ends_with(", 2 draw(s)"));
    CHECK(line_of(report, "queue frames in flight: ").ends_with(" of 2"));
}

TEST_CASE("texture writes are the draws of their pass and texels count as texture bytes") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const minemod_handle texture = texture_handle(1);
    const auto stream = SceneStream()
                            .define_texture(texture, 2, 1, 1, 0)
                            .write_texture(texture, {0, 0, 0, 1, 1}, red)
                            .write_texture(texture, {0, 1, 0, 1, 1}, white)
                            .begin_frame(dark_gray, 0)
                            .set_view(canvas_view(0))
                            .end_frame()
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const std::string report = metrics_text(session.get());
    CHECK(line_of(report, "pass write textures: ").ends_with(", 2 draw(s)"));
    CHECK(line_of(report, "pass interface: ").ends_with(", 0 draw(s)"));
    CHECK(line_of(report, "resource texture: ") == "resource texture: peak 8 byte(s) in 1 object(s)");
    CHECK(metrics_of(session.get()).peak_texture_bytes == 8);
    CHECK(metrics_of(session.get()).peak_staging_bytes > 0);
}

TEST_CASE("the world pass counts the terrain sections it drew besides its batches") {
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
    BatchState badge;
    badge.view = 1;
    const auto stream =
        SceneStream()
            .define_texture(atlas, one_texel.width, one_texel.height, 1, 0)
            .write_texture(atlas, {0, 0, 0, 1, 1}, white)
            .define_texture(lightmap, light_table.width, light_table.height, 1, MINEMOD_SAMPLING_LINEAR)
            .write_texture(lightmap, {0, 0, 0, light_table.width, light_table.height}, solid_texels(light_table, white))
            .publish_section(wall_section, wall_mesh())
            .begin_frame(dark_gray, MINEMOD_FRAME_DRAWS_WORLD)
            .set_terrain(terrain)
            .set_view(world_view)
            .set_view(canvas_view(1))
            .draw_batch(entity, colored_rectangle(upper_left_badge, red))
            .draw_batch(badge, colored_rectangle(upper_left_badge, white))
            .end_frame()
            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const std::string report = metrics_text(session.get());
    CHECK(line_of(report, "pass world: ").ends_with(", 2 draw(s)"));
    CHECK(line_of(report, "pass interface: ").ends_with(", 1 draw(s)"));
    CHECK(session.validation_text() == "");
}

TEST_CASE("metrics need a session and somewhere to be written") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    minemod_scene_metrics metrics{};

    CHECK(minemod_scene_session_get_metrics(nullptr, &metrics) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_scene_session_get_metrics(session.get(), nullptr) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_scene_session_read_metrics_text(nullptr, nullptr, 0) == 0);
}
