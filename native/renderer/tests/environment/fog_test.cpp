#include <doctest.h>

#include "environment/fog.hpp"

namespace {

using namespace minemod::renderer;

constexpr Fog morning_fog{{0.5f, 0.6f, 0.7f, 1.0f}, 10.0f, 30.0f};

}

TEST_CASE("fog is absent before its start, complete past its end and eases in between") {
    CHECK(fog_factor(morning_fog, 5.0f) == 0.0f);
    CHECK(fog_factor(morning_fog, 10.0f) == 0.0f);
    CHECK(fog_factor(morning_fog, 15.0f) == doctest::Approx(0.15625f));
    CHECK(fog_factor(morning_fog, 20.0f) == doctest::Approx(0.5f));
    CHECK(fog_factor(morning_fog, 25.0f) == doctest::Approx(0.84375f));
    CHECK(fog_factor(morning_fog, 30.0f) == 1.0f);
    CHECK(fog_factor(morning_fog, 45.0f) == 1.0f);
}

TEST_CASE("fog needs its end farther than its start") {
    CHECK(has_depth(morning_fog));
    CHECK_FALSE(has_depth(Fog{{}, 10.0f, 10.0f}));
    CHECK_FALSE(has_depth(Fog{{}, 10.0f, 5.0f}));
}

TEST_CASE("spherical fog measures the straight distance and cylindrical fog the farther of height and ground distance") {
    constexpr Vector3 high_and_near{3.0f, 12.0f, 4.0f};
    constexpr Vector3 low_and_far{6.0f, 2.0f, 8.0f};

    CHECK(fog_distance(FogShape::sphere, high_and_near) == doctest::Approx(13.0f));
    CHECK(fog_distance(FogShape::cylinder, high_and_near) == doctest::Approx(12.0f));
    CHECK(fog_distance(FogShape::cylinder, low_and_far) == doctest::Approx(10.0f));
}
