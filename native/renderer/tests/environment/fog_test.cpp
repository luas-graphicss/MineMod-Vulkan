#include <doctest.h>

#include "environment/fog.hpp"

namespace {

using namespace minemod::renderer;

constexpr Fog morning_fog{{0.5f, 0.6f, 0.7f, 1.0f}, 10.0f, 30.0f};

}

TEST_CASE("fog is absent before its start, complete past its end and linear in between") {
    CHECK(fog_factor(morning_fog, 5.0f) == 0.0f);
    CHECK(fog_factor(morning_fog, 10.0f) == 0.0f);
    CHECK(fog_factor(morning_fog, 20.0f) == doctest::Approx(0.5f));
    CHECK(fog_factor(morning_fog, 30.0f) == 1.0f);
    CHECK(fog_factor(morning_fog, 45.0f) == 1.0f);
}

TEST_CASE("fog needs its end farther than its start") {
    CHECK(has_depth(morning_fog));
    CHECK_FALSE(has_depth(Fog{{}, 10.0f, 10.0f}));
    CHECK_FALSE(has_depth(Fog{{}, 10.0f, 5.0f}));
}
