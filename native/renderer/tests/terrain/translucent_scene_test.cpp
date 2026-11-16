#include <array>
#include <cstddef>
#include <cstdint>
#include <span>
#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "frame/frame_readback.hpp"
#include "math/vector.hpp"
#include "support/pixel_colors.hpp"
#include "support/terrain_quads.hpp"
#include "support/terrain_stage.hpp"
#include "terrain/section_buffers.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/translucent_order.hpp"
#include "terrain/translucent_sections.hpp"
#include "upload/arena_layout.hpp"
#include "work/job_epochs.hpp"

namespace {

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

constexpr WorldPosition camera{8.0, 8.0, 0.0};
constexpr SectionPosition wall_section{0, 0, -3};
constexpr SectionPosition water_section{0, 0, -2};
constexpr VkOffset2D frame_center{32, 32};
constexpr VkDeviceSize index_alignment = 4;

constexpr std::array<float, 2> any_texel{0.5f, 0.5f};
constexpr std::array<std::uint16_t, 2> full_light{brightest_packed_light, brightest_packed_light};
constexpr std::array<std::uint8_t, 4> red{255, 0, 0, 255};
constexpr std::array<std::uint8_t, 4> half_yellow{255, 255, 0, 128};
constexpr std::array<std::uint8_t, 4> half_blue{0, 0, 255, 128};

constexpr QuadPlacement near_surface{0.0f, 0.0f, 16.0f, 16.0f, 12.0f};
constexpr QuadPlacement far_surface{0.0f, 0.0f, 16.0f, 16.0f, 4.0f};
constexpr QuadPlacement wall{0.0f, 0.0f, 16.0f, 16.0f, 8.0f};

SectionMesh wall_mesh() {
    SectionMesh mesh{};
    append_quad(mesh, TerrainLayer::solid, wall, {red, any_texel, full_light});
    return mesh;
}

SectionMesh water_with_the_near_surface_first() {
    SectionMesh mesh{};
    append_quad(mesh, TerrainLayer::translucent, near_surface, {half_blue, any_texel, full_light});
    append_quad(mesh, TerrainLayer::translucent, far_surface, {half_yellow, any_texel, full_light});
    return mesh;
}

void upload_order(TerrainStage& stage, const SectionPosition& section, std::span<const std::uint32_t> order) {
    stage.arena().reset();
    const ArenaRegion staged = stage.arena().stage(std::as_bytes(order), index_alignment).value();
    stage.device().submit([&](VkCommandBuffer commands) {
        stage.sections().find(section)->record_translucent_order(commands, stage.arena().buffer(), staged);
    });
}

void check_pixel(const FramePixels& pixels, const VkOffset2D& position, const Rgb& expected) {
    const Pixel drawn = pixels.at(static_cast<std::uint32_t>(position.x), static_cast<std::uint32_t>(position.y));
    CHECK_MESSAGE(is_close(drawn, expected), describe(drawn));
}

}

TEST_CASE("water surfaces of one section blend from the farthest once they are reordered for the camera") {
    TerrainStage stage;
    const SectionMesh water = water_with_the_near_surface_first();
    stage.publish(wall_section, wall_mesh());
    stage.publish(water_section, water);
    check_pixel(stage.render({camera}), frame_center, {127, 0, 128});

    TranslucentSections translucent;
    translucent.track(water_section, translucent_quads(water));
    upload_order(stage, water_section, translucent.reorder(water_section, camera));

    check_pixel(stage.render({camera}), frame_center, {127, 64, 128});
    CHECK(translucent.out_of_order(camera).empty());
    CHECK(stage.validation_text() == "");
}

TEST_CASE("an order that does not match the translucent indices of a section is rejected before it is recorded") {
    TerrainStage stage;
    stage.publish(wall_section, wall_mesh());
    stage.publish(water_section, water_with_the_near_surface_first());
    const SectionBuffers& opaque_only = *stage.sections().find(wall_section);
    const SectionBuffers& water = *stage.sections().find(water_section);

    CHECK_THROWS_AS(opaque_only.record_translucent_order(VK_NULL_HANDLE, VK_NULL_HANDLE, {0, 0}), std::invalid_argument);
    CHECK_THROWS_AS(water.record_translucent_order(VK_NULL_HANDLE, VK_NULL_HANDLE, {0, 24}), std::invalid_argument);
}
