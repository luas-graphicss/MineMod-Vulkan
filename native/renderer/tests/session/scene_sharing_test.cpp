#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <span>

#include <doctest.h>

#include "memory/exported_handle.hpp"
#include "minemod/abi/scene_commands.h"
#include "minemod/scene/scene_session.h"
#include "session/support/running_session.hpp"
#include "session/support/scene_shapes.hpp"
#include "session/support/scene_stream.hpp"
#include "session/support/shared_session.hpp"
#include "session/support/terrain_frames.hpp"
#include "support/open_handles.hpp"
#include "support/terrain_quads.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/terrain_layer.hpp"

namespace {

using namespace minemod::scene::test_support;
namespace renderer = minemod::renderer;
using minemod::interop::ExportedHandle;
using minemod::interop::test_support::open_handle_count;
using renderer::test_support::QuadPlacement;

constexpr VkExtent2D frame_extent{64, 64};
constexpr VkExtent2D wide_extent{128, 64};
constexpr std::uint64_t color_bytes = std::uint64_t{frame_extent.width} * frame_extent.height * 4;
constexpr int export_cycles = 50;
constexpr Color red{255, 0, 0, 255};
constexpr Color white{255, 255, 255, 255};
constexpr SectionKey wall_section{0, 0, -3, 0};
constexpr QuadPlacement wall_in_the_middle{5.4f, 0.0f, 10.6f, 16.0f, 8.0f};
constexpr VkRect2D upper_left_badge{{0, 0}, {16, 16}};

bool names_a_device(std::span<const std::uint8_t> uuid) {
    return std::ranges::any_of(uuid, [](std::uint8_t byte) { return byte != 0; });
}

renderer::SectionMesh wall_mesh() {
    renderer::SectionMesh mesh{};
    renderer::test_support::append_quad(
        mesh, renderer::TerrainLayer::solid, wall_in_the_middle, {red, {0.5f, 0.5f}, {240, 240}});
    return mesh;
}

std::vector<std::uint8_t> interface_frame(VkExtent2D extent) {
    ViewState view;
    view.projection = pixel_projection(extent);
    return SceneStream()
        .begin_frame(terrain_backdrop, 0)
        .set_view(view)
        .draw_batch(BatchState{}, colored_rectangle(upper_left_badge, white))
        .end_frame()
        .build();
}

void check_pixel(const DrawnFrame& frame, std::uint32_t x, std::uint32_t y, const Rgb& expected) {
    CHECK_MESSAGE(frame.shows(x, y, expected), frame.describe(x, y));
}

}

TEST_CASE("a session created for itself shares nothing") {
    RunningSession session(offscreen_config(frame_extent.width, frame_extent.height));
    minemod_scene_sharing sharing{};
    std::int64_t memory = -1;
    std::int64_t renderer_done = -1;
    std::int64_t provider_done = -1;

    CHECK(minemod_scene_session_get_sharing(session.get(), &sharing) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(
        minemod_scene_session_export_attachment(session.get(), MINEMOD_SHARED_ATTACHMENT_COLOR, &memory)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(
        minemod_scene_session_export_semaphores(session.get(), &renderer_done, &provider_done)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(memory == -1);
}

TEST_CASE("a shared session describes the color and depth-stencil attachments a provider imports") {
    RunningSession session(shared_config(frame_extent));

    const minemod_scene_sharing sharing = sharing_of(session);
    const minemod_shared_attachment& color = sharing.attachments[MINEMOD_SHARED_ATTACHMENT_COLOR];
    const minemod_shared_attachment& depth_stencil = sharing.attachments[MINEMOD_SHARED_ATTACHMENT_DEPTH_STENCIL];

    CHECK(color.vulkan_format == VK_FORMAT_R8G8B8A8_UNORM);
    CHECK(color.vulkan_layout == VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL);
    CHECK(color.width == frame_extent.width);
    CHECK(color.height == frame_extent.height);
    CHECK(color.allocation_bytes >= color_bytes);
    CHECK(depth_stencil.vulkan_format == VK_FORMAT_D32_SFLOAT_S8_UINT);
    CHECK(depth_stencil.vulkan_layout == VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL);
    CHECK(depth_stencil.width == frame_extent.width);
    CHECK(depth_stencil.height == frame_extent.height);
    CHECK(depth_stencil.allocation_bytes > color_bytes);
    CHECK(names_a_device(sharing.device_uuid));
    CHECK(names_a_device(sharing.driver_uuid));
    CHECK(sharing.generation == 1);
    CHECK(session.validation_text() == "");
}

TEST_CASE("the memory of each attachment is exported as a handle the caller owns") {
    RunningSession session(shared_config(frame_extent));
    const std::ptrdiff_t open_before = open_handle_count();
    std::int64_t memory = -1;

    for (int cycle = 0; cycle < export_cycles; ++cycle) {
        REQUIRE(exported_memory(session, MINEMOD_SHARED_ATTACHMENT_COLOR).is_open());
        REQUIRE(exported_memory(session, MINEMOD_SHARED_ATTACHMENT_DEPTH_STENCIL).is_open());
    }

    CHECK(open_handle_count() == open_before);
    CHECK(
        minemod_scene_session_export_attachment(session.get(), MINEMOD_SHARED_ATTACHMENT_COUNT, &memory)
        == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(session.validation_text() == "");
}

TEST_CASE("the handoff semaphores are exported or refused as the device reports") {
    RunningSession session(shared_config(frame_extent));
    std::int64_t renderer_done = -1;
    std::int64_t provider_done = -1;

    const minemod_result exported =
        minemod_scene_session_export_semaphores(session.get(), &renderer_done, &provider_done);

    if ((sharing_of(session).flags & MINEMOD_SCENE_SHARING_SEMAPHORES) == 0) {
        CHECK(exported == MINEMOD_RESULT_UNSUPPORTED_CAPABILITY);
        return;
    }
    REQUIRE(exported == MINEMOD_RESULT_OK);
    const ExportedHandle signaled_by_the_renderer(renderer_done);
    const ExportedHandle signaled_by_the_provider(provider_done);
    CHECK(signaled_by_the_renderer.is_open());
    CHECK(signaled_by_the_provider.is_open());
    CHECK(signaled_by_the_renderer.get() != signaled_by_the_provider.get());
    CHECK(session.validation_text() == "");
}

TEST_CASE("a shared session draws terrain and interface into the attachments it shares") {
    RunningSession session(shared_config(frame_extent));
    REQUIRE(session.submit(terrain_textures()) == MINEMOD_RESULT_OK);
    SceneStream loaded;
    loaded.publish_section(wall_section, wall_mesh());

    REQUIRE(session.submit(world_frame(loaded)) == MINEMOD_RESULT_OK);
    const DrawnFrame world = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(world, 32, 32, {255, 0, 0});
    check_pixel(world, 4, 32, terrain_backdrop_pixel);

    REQUIRE(session.submit(interface_frame(frame_extent)) == MINEMOD_RESULT_OK);
    const DrawnFrame interface = session.read_frame(frame_extent.width, frame_extent.height);
    check_pixel(interface, 8, 8, {255, 255, 255});
    check_pixel(interface, 32, 32, terrain_backdrop_pixel);
    CHECK(session.validation_text() == "");
}

TEST_CASE("resizing a shared session replaces its attachments under a new generation") {
    RunningSession session(shared_config(frame_extent));
    REQUIRE(session.submit(interface_frame(frame_extent)) == MINEMOD_RESULT_OK);

    REQUIRE(
        minemod_scene_session_resize(session.get(), wide_extent.width, wide_extent.height) == MINEMOD_RESULT_OK);

    const minemod_scene_sharing sharing = sharing_of(session);
    CHECK(sharing.generation == 2);
    CHECK(sharing.attachments[MINEMOD_SHARED_ATTACHMENT_COLOR].width == wide_extent.width);
    CHECK(sharing.attachments[MINEMOD_SHARED_ATTACHMENT_DEPTH_STENCIL].width == wide_extent.width);
    CHECK(exported_memory(session, MINEMOD_SHARED_ATTACHMENT_COLOR).is_open());
    REQUIRE(session.submit(interface_frame(wide_extent)) == MINEMOD_RESULT_OK);
    const DrawnFrame resized = session.read_frame(wide_extent.width, wide_extent.height);
    check_pixel(resized, 8, 8, {255, 255, 255});
    check_pixel(resized, 100, 32, terrain_backdrop_pixel);
    CHECK(session.validation_text() == "");
}
