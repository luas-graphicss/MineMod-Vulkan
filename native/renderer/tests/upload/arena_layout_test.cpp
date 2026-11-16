#include <stdexcept>

#include <doctest.h>

#include "upload/arena_layout.hpp"

using namespace minemod::renderer;

TEST_CASE("regions are claimed one after the other at the requested alignment") {
    ArenaLayout layout(64);

    CHECK(layout.claim(3, 1) == ArenaRegion{0, 3});
    CHECK(layout.claim(8, 8) == ArenaRegion{8, 8});
    CHECK(layout.used() == 16);
}

TEST_CASE("a region that does not fit is refused without consuming the arena") {
    ArenaLayout layout(16);
    REQUIRE(layout.claim(10, 1).has_value());

    CHECK_FALSE(layout.claim(7, 1).has_value());
    CHECK_FALSE(layout.claim(4, 16).has_value());
    CHECK(layout.used() == 10);
    CHECK(layout.claim(6, 1) == ArenaRegion{10, 6});
}

TEST_CASE("a reset makes the whole capacity available again") {
    ArenaLayout layout(8);
    REQUIRE(layout.claim(8, 1).has_value());

    layout.reset();

    CHECK(layout.claim(8, 4) == ArenaRegion{0, 8});
}

TEST_CASE("empty regions and alignments that are not a power of two are rejected") {
    ArenaLayout layout(8);

    CHECK_THROWS_AS(layout.claim(0, 1), std::invalid_argument);
    CHECK_THROWS_AS(layout.claim(4, 3), std::invalid_argument);
    CHECK_THROWS_AS(layout.claim(4, 0), std::invalid_argument);
}
