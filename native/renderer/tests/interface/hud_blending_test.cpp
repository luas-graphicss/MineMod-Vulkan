#include <doctest.h>

#include "pipelines/blending.hpp"

namespace {

using namespace minemod::renderer;

}

TEST_CASE("inversion weighs each color by the opposite of the other and replaces the alpha") {
    const BlendFactors factors = blend_factors(Blending::inversion).value();

    CHECK(factors.source_color == VK_BLEND_FACTOR_ONE_MINUS_DST_COLOR);
    CHECK(factors.destination_color == VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR);
    CHECK(factors.source_alpha == VK_BLEND_FACTOR_ONE);
    CHECK(factors.destination_alpha == VK_BLEND_FACTOR_ZERO);
}

TEST_CASE("darkening adds no color of its own and removes the source from what was drawn") {
    const BlendFactors factors = blend_factors(Blending::darkening).value();

    CHECK(factors.source_color == VK_BLEND_FACTOR_ZERO);
    CHECK(factors.destination_color == VK_BLEND_FACTOR_ONE_MINUS_SRC_COLOR);
    CHECK(factors.source_alpha == VK_BLEND_FACTOR_ONE);
    CHECK(factors.destination_alpha == VK_BLEND_FACTOR_ZERO);
}
