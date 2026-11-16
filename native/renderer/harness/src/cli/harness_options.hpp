#pragma once

#include <span>
#include <stdexcept>
#include <string_view>

#include "run/harness_run.hpp"

namespace minemod::harness {

class UsageError : public std::runtime_error {
public:
    using std::runtime_error::runtime_error;
};

inline constexpr std::string_view harness_usage = "usage: minemod_harness [--validation] [--frames <count>]";

HarnessOptions parse_harness_options(std::span<const std::string_view> arguments);

}
