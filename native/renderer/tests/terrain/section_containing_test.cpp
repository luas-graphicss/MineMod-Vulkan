#include <limits>
#include <stdexcept>

#include <doctest.h>

#include "math/vector.hpp"
#include "terrain/section_space.hpp"
#include "work/job_epochs.hpp"

namespace {

using namespace minemod::renderer;

}

TEST_CASE("a position lies in the section whose sixteen blocks contain it") {
    CHECK(section_containing({8.0, 8.0, 0.0}) == SectionPosition{0, 0, 0});
    CHECK(section_containing({15.99, 16.0, 47.5}) == SectionPosition{0, 1, 2});
}

TEST_CASE("positions below zero lie in sections counted downward") {
    CHECK(section_containing({-0.5, -16.0, -16.01}) == SectionPosition{-1, -1, -2});
}

TEST_CASE("a position that is not finite lies in no section") {
    const double not_a_number = std::numeric_limits<double>::quiet_NaN();
    const double infinity = std::numeric_limits<double>::infinity();

    CHECK_THROWS_AS(section_containing({not_a_number, 0.0, 0.0}), std::invalid_argument);
    CHECK_THROWS_AS(section_containing({0.0, infinity, 0.0}), std::invalid_argument);
}
