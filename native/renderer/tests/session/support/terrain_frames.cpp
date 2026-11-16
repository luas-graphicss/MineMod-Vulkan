#include "session/support/terrain_frames.hpp"

#include <cmath>

#include <vulkan/vulkan.h>

#include "math/matrix.hpp"
#include "minemod/abi/scene_commands.h"
#include "session/support/scene_shapes.hpp"

namespace minemod::scene::test_support {

namespace {

constexpr VkExtent2D one_texel{1, 1};
constexpr VkExtent2D light_table{16, 16};
constexpr Color white{255, 255, 255, 255};
constexpr std::uint32_t atlas_index = 1;
constexpr std::uint32_t lightmap_index = 2;
constexpr float view_distance = 40.0f;
constexpr float view_reach = 8.0f;
constexpr float nearest_drawn = 0.5f;
constexpr float farthest_drawn = 100.0f;

}

std::vector<std::uint8_t> terrain_textures() {
    const minemod_handle atlas = texture_handle(atlas_index);
    const minemod_handle lightmap = texture_handle(lightmap_index);
    return SceneStream()
        .define_texture(atlas, one_texel.width, one_texel.height, 1, 0)
        .write_texture(atlas, {0, 0, 0, one_texel.width, one_texel.height}, white)
        .define_texture(lightmap, light_table.width, light_table.height, 1, MINEMOD_SAMPLING_LINEAR)
        .write_texture(lightmap, {0, 0, 0, light_table.width, light_table.height}, solid_texels(light_table, white))
        .begin_frame(terrain_backdrop, 0)
        .end_frame()
        .build();
}

TerrainState terrain_seen_from(const std::array<double, 3>& camera) {
    TerrainState terrain;
    terrain.view_projection = renderer::perspective(
                                  {2.0f * std::atan(view_reach / view_distance), 1.0f, nearest_drawn, farthest_drawn})
                                  .columns;
    terrain.camera = camera;
    terrain.atlas = texture_handle(atlas_index);
    terrain.lightmap = texture_handle(lightmap_index);
    return terrain;
}

std::vector<std::uint8_t> world_frame(SceneStream stream, const std::array<double, 3>& camera) {
    return stream.begin_frame(terrain_backdrop, MINEMOD_FRAME_DRAWS_WORLD)
        .set_terrain(terrain_seen_from(camera))
        .end_frame()
        .build();
}

}
