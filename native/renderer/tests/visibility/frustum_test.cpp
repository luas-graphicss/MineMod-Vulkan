#include <numbers>

#include <doctest.h>

#include "math/box.hpp"
#include "math/matrix.hpp"
#include "visibility/frustum.hpp"

namespace {

using namespace minemod::renderer;

constexpr float half_turn = std::numbers::pi_v<float>;
constexpr PerspectiveLens square_lens{half_turn / 2.0f, 1.0f, 1.0f, 100.0f};

Box box_around(const Vector3& center, float reach) {
    return Box{
        {center.x - reach, center.y - reach, center.z - reach},
        {center.x + reach, center.y + reach, center.z + reach},
    };
}

}

TEST_CASE("a box in front of the camera is inside the frustum") {
    const Frustum frustum(perspective(square_lens));

    CHECK(frustum.intersects(box_around({0.0f, 0.0f, -10.0f}, 1.0f)));
}

TEST_CASE("boxes behind the camera, past the far distance or off to a side are outside the frustum") {
    const Frustum frustum(perspective(square_lens));

    CHECK_FALSE(frustum.intersects(box_around({0.0f, 0.0f, 10.0f}, 1.0f)));
    CHECK_FALSE(frustum.intersects(box_around({0.0f, 0.0f, -200.0f}, 1.0f)));
    CHECK_FALSE(frustum.intersects(box_around({50.0f, 0.0f, -10.0f}, 1.0f)));
    CHECK_FALSE(frustum.intersects(box_around({-50.0f, 0.0f, -10.0f}, 1.0f)));
    CHECK_FALSE(frustum.intersects(box_around({0.0f, 50.0f, -10.0f}, 1.0f)));
    CHECK_FALSE(frustum.intersects(box_around({0.0f, -50.0f, -10.0f}, 1.0f)));
}

TEST_CASE("a box that straddles a frustum plane still intersects") {
    const Frustum frustum(perspective(square_lens));

    CHECK(frustum.intersects(box_around({10.0f, 0.0f, -10.0f}, 1.0f)));
    CHECK(frustum.intersects(box_around({0.0f, 0.0f, -1.0f}, 0.5f)));
    CHECK(frustum.intersects(box_around({0.0f, 0.0f, -100.0f}, 0.5f)));
}

TEST_CASE("turning the camera around swaps what is in front of it") {
    const Frustum frustum(multiply(perspective(square_lens), rotation_about_y(half_turn)));

    CHECK(frustum.intersects(box_around({0.0f, 0.0f, 10.0f}, 1.0f)));
    CHECK_FALSE(frustum.intersects(box_around({0.0f, 0.0f, -10.0f}, 1.0f)));
}
