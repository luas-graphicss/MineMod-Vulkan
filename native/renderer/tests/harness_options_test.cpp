#include <array>
#include <string_view>

#include <doctest.h>

#include "cli/harness_options.hpp"

namespace {

using namespace minemod::harness;
using minemod::renderer::Validation;

}

TEST_CASE("the harness options default to one unvalidated frame") {
    const HarnessOptions options = parse_harness_options({});

    CHECK(options.validation == Validation::disabled);
    CHECK(options.presented_frames == 1);
}

TEST_CASE("the harness options read validation and the frame count") {
    const std::array<std::string_view, 3> arguments{"--frames", "12", "--validation"};

    const HarnessOptions options = parse_harness_options(arguments);

    CHECK(options.validation == Validation::required);
    CHECK(options.presented_frames == 12);
}

TEST_CASE("the harness options reject what they do not understand") {
    const std::array<std::string_view, 1> unknown{"--fullscreen"};
    const std::array<std::string_view, 1> incomplete{"--frames"};
    const std::array<std::string_view, 2> negative{"--frames", "-1"};
    const std::array<std::string_view, 2> excessive{"--frames", "10001"};

    CHECK_THROWS_AS(parse_harness_options(unknown), UsageError);
    CHECK_THROWS_AS(parse_harness_options(incomplete), UsageError);
    CHECK_THROWS_AS(parse_harness_options(negative), UsageError);
    CHECK_THROWS_AS(parse_harness_options(excessive), UsageError);
}
