#include <cstddef>

#include <doctest.h>

#include "terrain/terrain_layer.hpp"

namespace {

using namespace minemod::renderer;

}

TEST_CASE("terrain layers are drawn opaque first and translucent last") {
    CHECK(terrain_layers.front() == TerrainLayer::solid);
    CHECK(terrain_layers.back() == TerrainLayer::translucent);
    for (std::size_t position = 0; position < terrain_layers.size(); ++position) {
        CHECK(layer_index(terrain_layers[position]) == position);
    }
}

TEST_CASE("only the translucent layer blends and only it is drawn back to front") {
    for (const TerrainLayer layer : terrain_layers) {
        const LayerState state = layer_state(layer);
        const bool is_translucent = layer == TerrainLayer::translucent;

        CHECK((state.blending == Blending::alpha) == is_translucent);
        CHECK((state.direction == DrawDirection::back_to_front) == is_translucent);
        CHECK(state.depth == terrain_depth);
    }
}

TEST_CASE("cutout layers discard below the thresholds of the reference renderer") {
    CHECK(layer_state(TerrainLayer::solid).alpha_cutoff == 0.0f);
    CHECK(layer_state(TerrainLayer::cutout_mipped).alpha_cutoff == 0.5f);
    CHECK(layer_state(TerrainLayer::cutout).alpha_cutoff == 0.1f);
    CHECK(layer_state(TerrainLayer::translucent).alpha_cutoff == 0.0f);
}

TEST_CASE("only the unmipped cutout layer samples the base level of the atlas") {
    for (const TerrainLayer layer : terrain_layers) {
        const bool samples_base_only = layer_state(layer).atlas_levels == AtlasLevels::base_only;

        CHECK(samples_base_only == (layer == TerrainLayer::cutout));
    }
}
