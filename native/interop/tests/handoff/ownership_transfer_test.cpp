#include <cstdint>
#include <stdexcept>

#include <doctest.h>

#include "handoff/ownership_transfer.hpp"

namespace {

using namespace minemod::interop;

constexpr std::uint32_t any_queue_family = 0;

}

TEST_CASE("a transfer that moves no image and no buffer is rejected in both directions") {
    CHECK_THROWS_AS(OwnershipTransfer::release_from(any_queue_family, HandoffResources{}), std::invalid_argument);
    CHECK_THROWS_AS(OwnershipTransfer::acquire_for(any_queue_family, HandoffResources{}), std::invalid_argument);
}
