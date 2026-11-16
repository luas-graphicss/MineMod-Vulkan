#pragma once

#include <iosfwd>

#include "run/harness_run.hpp"

namespace minemod::harness {

void write_harness_report(std::ostream& output, const HarnessReport& report);

}
