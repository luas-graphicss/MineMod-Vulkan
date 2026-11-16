#include <doctest.h>

#include "draw/texture_transform.hpp"
#include "math/matrix.hpp"

namespace {

using namespace minemod::renderer;

constexpr Matrix4 scaled_and_moved{{
    2.0f, 0.0f, 0.0f, 0.0f,
    0.0f, 3.0f, 0.0f, 0.0f,
    0.0f, 0.0f, 1.0f, 0.0f,
    0.25f, 0.5f, 0.0f, 1.0f,
}};

constexpr Matrix4 quarter_turn{{
    0.0f, 1.0f, 0.0f, 0.0f,
    -1.0f, 0.0f, 0.0f, 0.0f,
    0.0f, 0.0f, 1.0f, 0.0f,
    0.0f, 0.0f, 0.0f, 1.0f,
}};

}

TEST_CASE("a batch that sets no texture transform samples its coordinates unchanged") {
    CHECK(texture_transform(identity_matrix) == TextureTransform{});
}

TEST_CASE("a texture matrix keeps its scale and its translation along both coordinates") {
    CHECK(texture_transform(scaled_and_moved) == TextureTransform{{2.0f, 0.0f, 0.25f}, {0.0f, 3.0f, 0.5f}});
}

TEST_CASE("a rotated texture matrix mixes one coordinate into the other") {
    CHECK(texture_transform(quarter_turn) == TextureTransform{{0.0f, -1.0f, 0.0f}, {1.0f, 0.0f, 0.0f}});
}
