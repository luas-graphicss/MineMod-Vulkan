#include <doctest.h>

#include "pipelines/blending.hpp"

namespace {

using namespace minemod::renderer;

}

TEST_CASE("opaque drawing has no blend factors") {
    CHECK_FALSE(blend_factors(Blending::disabled).has_value());
}

TEST_CASE("alpha blending weighs the source by its alpha and accumulates coverage") {
    const BlendFactors factors = blend_factors(Blending::alpha).value();

    CHECK(factors.source_color == VK_BLEND_FACTOR_SRC_ALPHA);
    CHECK(factors.destination_color == VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
    CHECK(factors.source_alpha == VK_BLEND_FACTOR_ONE);
    CHECK(factors.destination_alpha == VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA);
}

TEST_CASE("additive and lightning blending keep everything already drawn") {
    CHECK(blend_factors(Blending::additive).value().destination_color == VK_BLEND_FACTOR_ONE);
    CHECK(blend_factors(Blending::additive).value().source_color == VK_BLEND_FACTOR_ONE);
    CHECK(blend_factors(Blending::lightning).value().destination_color == VK_BLEND_FACTOR_ONE);
    CHECK(blend_factors(Blending::lightning).value().source_color == VK_BLEND_FACTOR_SRC_ALPHA);
}

TEST_CASE("glint leaves the destination alpha alone and crumbling multiplies both colors") {
    const BlendFactors glint = blend_factors(Blending::glint).value();
    const BlendFactors crumbling = blend_factors(Blending::crumbling).value();

    CHECK(glint.source_alpha == VK_BLEND_FACTOR_ZERO);
    CHECK(glint.destination_alpha == VK_BLEND_FACTOR_ONE);
    CHECK(crumbling.source_color == VK_BLEND_FACTOR_DST_COLOR);
    CHECK(crumbling.destination_color == VK_BLEND_FACTOR_SRC_COLOR);
}
