#include <array>
#include <cstdint>
#include <vector>

#include <doctest.h>

#include "minemod/abi/command_stream.h"
#include "minemod/abi/scene_commands.h"
#include "session/support/running_session.hpp"
#include "session/support/scene_shapes.hpp"
#include "session/support/scene_stream.hpp"
#include "support/stream_builder.hpp"
#include "support/terrain_quads.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/terrain_layer.hpp"

namespace {

using namespace minemod::scene::test_support;
namespace renderer = minemod::renderer;

constexpr VkExtent2D frame_extent{32, 32};
constexpr VkExtent2D resized_extent{48, 16};
constexpr VkExtent2D large_texture{64, 64};
constexpr std::uint64_t smallest_arena = MINEMOD_SCENE_MIN_ARENA_BYTES;
constexpr std::array<float, 4> black{0.0f, 0.0f, 0.0f, 1.0f};
constexpr Color red{255, 0, 0, 255};
constexpr Color white{255, 255, 255, 255};
constexpr VkRect2D corner{{0, 0}, {8, 8}};
constexpr SectionKey section_of_first_world{2, 0, 5, 0};
constexpr SectionKey section_of_second_world{2, 0, 5, 1};

renderer::SectionMesh one_quad() {
    renderer::SectionMesh mesh{};
    renderer::test_support::append_quad(
        mesh, renderer::TerrainLayer::solid, {0.0f, 0.0f, 16.0f, 16.0f, 8.0f}, {red, {0.5f, 0.5f}, {240, 240}});
    return mesh;
}

ViewState canvas_view() {
    ViewState state;
    state.projection = pixel_projection(frame_extent);
    return state;
}

std::vector<std::uint8_t> interface_frame() {
    return SceneStream()
        .begin_frame(black, 0)
        .set_view(canvas_view())
        .draw_batch(BatchState{}, colored_rectangle(corner, red))
        .end_frame()
        .build();
}

}

TEST_CASE("a section meshed for a world that was left is dropped") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = SceneStream()
                            .change_world()
                            .publish_section(section_of_first_world, one_quad())
                            .publish_section(section_of_second_world, one_quad())
                            .build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    const minemod_scene_counters counters = session.counters();
    CHECK(counters.sections_dropped == 1);
    CHECK(counters.sections_published == 1);
    CHECK(counters.live_sections == 1);
}

TEST_CASE("changing the world unloads every section") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream =
        SceneStream().publish_section(section_of_first_world, one_quad()).change_world().build();

    REQUIRE(session.submit(stream) == MINEMOD_RESULT_OK);

    CHECK(session.counters().live_sections == 0);
}

TEST_CASE("a command outside its frame is refused and the session keeps drawing") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto orphan = SceneStream().draw_batch(BatchState{}, colored_rectangle(corner, red)).build();
    minemod_stream_report report{};

    CHECK(session.submit(orphan, report) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(report.failed_command == 0);

    REQUIRE(session.submit(interface_frame()) == MINEMOD_RESULT_OK);
    CHECK(session.read_frame(frame_extent.width, frame_extent.height).shows(4, 4, {255, 0, 0}));
    CHECK(session.validation_text() == "");
}

TEST_CASE("a frame is refused when it reuses a view, opens twice or draws a world without terrain") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    SceneStream stream;

    SUBCASE("a view set twice") {
        stream.begin_frame(black, 0).set_view(canvas_view()).set_view(canvas_view());
    }
    SUBCASE("a frame opened twice") {
        stream.begin_frame(black, 0).begin_frame(black, 0);
    }
    SUBCASE("a world without terrain") {
        stream.begin_frame(black, MINEMOD_FRAME_DRAWS_WORLD).end_frame();
    }
    SUBCASE("a world batch in a frame without a world") {
        BatchState solid;
        solid.slot = MINEMOD_BATCH_SOLID;
        stream.begin_frame(black, 0).set_view(canvas_view()).draw_batch(solid, colored_rectangle(corner, red));
    }
    SUBCASE("a frame ended before it began") {
        stream.end_frame();
    }

    CHECK(session.submit(stream.build()) == MINEMOD_RESULT_INVALID_ARGUMENT);
}

TEST_CASE("texels that do not fit the upload arena of a frame are refused") {
    minemod_scene_session_config config = offscreen_config(frame_extent.width, frame_extent.height);
    config.upload_bytes_per_frame = smallest_arena;
    RunningSession session(config);
    const minemod_handle texture = texture_handle(1);
    const auto stream =
        SceneStream()
            .define_texture(texture, large_texture.width, large_texture.height, 1, 0)
            .write_texture(
                texture, {0, 0, 0, large_texture.width, large_texture.height}, solid_texels(large_texture, white))
            .build();
    minemod_stream_report report{};

    CHECK(session.submit(stream, report) == MINEMOD_RESULT_LIMIT_EXCEEDED);
    CHECK(report.failed_command == 1);
}

TEST_CASE("a texture write must lie inside its level and carry its texels") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const minemod_handle texture = texture_handle(1);
    SceneStream stream;
    stream.define_texture(texture, 2, 2, 2, 0);

    SUBCASE("a region past the right edge") {
        stream.write_texture(texture, {0, 1, 0, 2, 1}, solid_texels({2, 1}, white));
    }
    SUBCASE("a level the texture does not have") {
        stream.write_texture(texture, {2, 0, 0, 1, 1}, white);
    }
    SUBCASE("fewer texels than the region") {
        stream.write_texture(texture, {0, 0, 0, 2, 2}, white);
    }
    SUBCASE("a region of the base level written to the smaller level") {
        stream.write_texture(texture, {1, 0, 0, 2, 2}, solid_texels({2, 2}, white));
    }

    CHECK(session.submit(stream.build()) == MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS);
}

TEST_CASE("handles the session does not know are refused") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const minemod_handle texture = texture_handle(1);
    SceneStream stream;

    SUBCASE("a write to a texture that was never defined") {
        stream.write_texture(texture, {0, 0, 0, 1, 1}, white);
    }
    SUBCASE("a texture defined twice") {
        stream.define_texture(texture, 1, 1, 1, 0).define_texture(texture, 1, 1, 1, 0);
    }
    SUBCASE("a handle of another kind") {
        stream.define_texture(minemod::abi::testing::buffer_handle(1), 1, 1, 1, 0);
    }
    SUBCASE("a texture used after it was destroyed") {
        stream.define_texture(texture, 1, 1, 1, 0).destroy_texture(texture).write_texture(texture, {0, 0, 0, 1, 1}, white);
    }
    SUBCASE("a batch that samples an unknown texture") {
        BatchState batch;
        batch.texture = texture;
        stream.begin_frame(black, 0).set_view(canvas_view()).draw_batch(batch, colored_rectangle(corner, red));
    }

    CHECK(session.submit(stream.build()) == MINEMOD_RESULT_INVALID_HANDLE);
}

TEST_CASE("a texture shape the renderer cannot hold is malformed") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const minemod_handle texture = texture_handle(1);
    SceneStream stream;

    SUBCASE("no texels") {
        stream.define_texture(texture, 0, 4, 1, 0);
    }
    SUBCASE("more levels than its extent has") {
        stream.define_texture(texture, 4, 4, 4, 0);
    }
    SUBCASE("unknown sampling bits") {
        stream.define_texture(texture, 4, 4, 1, 0x80);
    }

    CHECK(session.submit(stream.build()) == MINEMOD_RESULT_MALFORMED_STREAM);
}

TEST_CASE("a section mesh whose indices point past its vertices is malformed") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    renderer::SectionMesh mesh = one_quad();
    mesh.indices.front() = static_cast<std::uint32_t>(mesh.vertices.size());

    CHECK(
        session.submit(SceneStream().publish_section(section_of_first_world, mesh).build())
        == MINEMOD_RESULT_MALFORMED_STREAM);
    CHECK(session.counters().live_sections == 0);
}

TEST_CASE("a destroyed texture stops counting while the frame that drew it finishes") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const minemod_handle texture = texture_handle(1);
    BatchState textured;
    textured.elements = MINEMOD_VERTEX_POSITION | MINEMOD_VERTEX_COLOR | MINEMOD_VERTEX_TEXTURE;
    textured.shading = MINEMOD_SHADING_TEXTURE;
    textured.texture = texture;
    const auto drawn = SceneStream()
                           .define_texture(texture, 1, 1, 1, 0)
                           .write_texture(texture, {0, 0, 0, 1, 1}, red)
                           .begin_frame(black, 0)
                           .set_view(canvas_view())
                           .draw_batch(textured, textured_rectangle(corner, white))
                           .end_frame()
                           .destroy_texture(texture)
                           .build();

    REQUIRE(session.submit(drawn) == MINEMOD_RESULT_OK);
    CHECK(session.counters().live_textures == 0);
    CHECK(session.counters().live_texture_bytes == 0);

    REQUIRE(session.submit(interface_frame()) == MINEMOD_RESULT_OK);
    REQUIRE(session.submit(interface_frame()) == MINEMOD_RESULT_OK);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a scene session refuses buffer commands") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    const auto stream = minemod::abi::testing::StreamBuilder()
                            .create_buffer(minemod::abi::testing::buffer_handle(0), 16, MINEMOD_BUFFER_USAGE_VERTEX)
                            .build();

    CHECK(session.submit(stream) == MINEMOD_RESULT_UNSUPPORTED_CAPABILITY);
}

TEST_CASE("a resized session draws frames of its new extent") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(session.submit(interface_frame()) == MINEMOD_RESULT_OK);

    REQUIRE(minemod_scene_session_resize(session.get(), resized_extent.width, resized_extent.height) == MINEMOD_RESULT_OK);
    std::vector<std::uint8_t> pixels(std::size_t{resized_extent.width} * resized_extent.height * 4);
    CHECK(
        minemod_scene_session_read_frame(session.get(), pixels.data(), pixels.size())
        == MINEMOD_RESULT_INVALID_ARGUMENT);

    REQUIRE(session.submit(interface_frame()) == MINEMOD_RESULT_OK);
    CHECK(minemod_scene_session_read_frame(session.get(), pixels.data(), pixels.size()) == MINEMOD_RESULT_OK);
    CHECK(
        minemod_scene_session_read_frame(session.get(), pixels.data(), pixels.size() - 4)
        == MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a session is not resized while a frame is open") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    REQUIRE(session.submit(SceneStream().begin_frame(black, 0).build()) == MINEMOD_RESULT_OK);

    CHECK(
        minemod_scene_session_resize(session.get(), resized_extent.width, resized_extent.height)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
}

TEST_CASE("a session configuration outside the limits is refused before anything is created") {
    minemod_scene_host* host = create_validated_host({});
    minemod_scene_session_config config = offscreen_config(frame_extent.width, frame_extent.height);
    minemod_result expected = MINEMOD_RESULT_INVALID_ARGUMENT;

    SUBCASE("a struct of another size") {
        config.struct_bytes -= 8;
    }
    SUBCASE("a frame without area") {
        config.height = 0;
    }
    SUBCASE("no texture allowed") {
        config.max_textures = 0;
    }
    SUBCASE("a frame wider than the limit") {
        config.width = MINEMOD_SCENE_MAX_EXTENT + 1;
        expected = MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    SUBCASE("an upload arena below the minimum") {
        config.upload_bytes_per_frame = smallest_arena - 1;
        expected = MINEMOD_RESULT_LIMIT_EXCEEDED;
    }

    minemod_scene_session* session = nullptr;
    CHECK(minemod_scene_session_create(host, &config, &session) == expected);
    CHECK(session == nullptr);
    minemod_scene_host_destroy(host);
}
