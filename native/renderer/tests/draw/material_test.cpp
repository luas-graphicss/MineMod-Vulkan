#include <doctest.h>

#include "draw/material.hpp"

namespace {

using namespace minemod::renderer;

}

TEST_CASE("each shading step is told to the shaders by its own bit") {
    CHECK(shading_bits(Shading{}) == 0u);
    CHECK(shading_bits(Shading{.samples_texture = true}) == 1u);
    CHECK(shading_bits(Shading{.applies_lightmap = true}) == 2u);
    CHECK(shading_bits(Shading{.applies_overlay = true}) == 4u);
    CHECK(shading_bits(Shading{.shades_by_normal = true}) == 8u);
    CHECK(shading_bits(Shading{.fades_into_fog = true}) == 16u);
    CHECK(shading_bits(Shading{true, true, true, true, true}) == 31u);
}
