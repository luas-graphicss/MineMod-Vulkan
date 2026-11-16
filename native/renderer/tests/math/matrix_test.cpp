#include <numbers>
#include <stdexcept>

#include <doctest.h>

#include "math/matrix.hpp"

namespace {

using namespace minemod::renderer;

constexpr float quarter_turn = std::numbers::pi_v<float> / 2.0f;
constexpr PerspectiveLens wide_lens{quarter_turn, 2.0f, 1.0f, 101.0f};

struct ClipPoint {
    float x;
    float y;
    float z;
    float w;
};

ClipPoint transform(const Matrix4& matrix, float x, float y, float z) {
    return ClipPoint{
        matrix.at(0, 0) * x + matrix.at(0, 1) * y + matrix.at(0, 2) * z + matrix.at(0, 3),
        matrix.at(1, 0) * x + matrix.at(1, 1) * y + matrix.at(1, 2) * z + matrix.at(1, 3),
        matrix.at(2, 0) * x + matrix.at(2, 1) * y + matrix.at(2, 2) * z + matrix.at(2, 3),
        matrix.at(3, 0) * x + matrix.at(3, 1) * y + matrix.at(3, 2) * z + matrix.at(3, 3),
    };
}

}

TEST_CASE("a perspective lens maps its near and far distances to the ends of the depth range") {
    const Matrix4 projection = perspective(wide_lens);

    const ClipPoint nearest = transform(projection, 0.0f, 0.0f, -wide_lens.near_distance);
    const ClipPoint farthest = transform(projection, 0.0f, 0.0f, -wide_lens.far_distance);

    CHECK(nearest.z / nearest.w == doctest::Approx(0.0f));
    CHECK(farthest.z / farthest.w == doctest::Approx(1.0f));
}

TEST_CASE("a perspective lens keeps up pointing up and fits the wider axis to the aspect ratio") {
    const Matrix4 projection = perspective(wide_lens);

    const ClipPoint upper_right = transform(projection, 2.0f, 1.0f, -1.0f);

    CHECK(upper_right.x / upper_right.w == doctest::Approx(1.0f));
    CHECK(upper_right.y / upper_right.w == doctest::Approx(1.0f));
}

TEST_CASE("multiplying by the identity leaves a matrix unchanged") {
    const Matrix4 projection = perspective(wide_lens);

    CHECK(multiply(projection, identity_matrix).columns == projection.columns);
    CHECK(multiply(identity_matrix, projection).columns == projection.columns);
}

TEST_CASE("a quarter turn about the vertical axis carries the forward direction to the left") {
    const ClipPoint turned = transform(rotation_about_y(quarter_turn), 0.0f, 0.0f, -1.0f);

    CHECK(turned.x == doctest::Approx(-1.0f));
    CHECK(turned.y == doctest::Approx(0.0f));
    CHECK(turned.z == doctest::Approx(0.0f));
}

TEST_CASE("a lens that cannot form an image is rejected") {
    CHECK_THROWS_AS(perspective({0.0f, 1.0f, 1.0f, 2.0f}), std::invalid_argument);
    CHECK_THROWS_AS(perspective({quarter_turn, 0.0f, 1.0f, 2.0f}), std::invalid_argument);
    CHECK_THROWS_AS(perspective({quarter_turn, 1.0f, 0.0f, 2.0f}), std::invalid_argument);
    CHECK_THROWS_AS(perspective({quarter_turn, 1.0f, 2.0f, 2.0f}), std::invalid_argument);
}
