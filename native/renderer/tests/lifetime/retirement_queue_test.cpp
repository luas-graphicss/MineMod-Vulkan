#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "lifetime/retirement_queue.hpp"
#include "support/tracked_resource.hpp"

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

TEST_CASE("a retired resource stays alive until its last frame completes") {
    std::vector<int> released;
    RetirementQueue queue;
    queue.retire(4, TrackedResource(released, 1));

    CHECK(queue.release_through(3) == 0);
    CHECK(released.empty());
    CHECK(queue.release_through(4) == 1);
    CHECK(released == std::vector<int>{1});
    CHECK(queue.pending_count() == 0);
}

TEST_CASE("resources leave in the order they were retired") {
    std::vector<int> released;
    RetirementQueue queue;
    queue.retire(1, TrackedResource(released, 1));
    queue.retire(1, TrackedResource(released, 2));
    queue.retire(2, TrackedResource(released, 3));

    CHECK(queue.release_through(1) == 2);
    CHECK(released == std::vector<int>{1, 2});
    CHECK(queue.pending_count() == 1);
}

TEST_CASE("a resource cannot be retired for a frame older than the previous one") {
    std::vector<int> released;
    RetirementQueue queue;
    queue.retire(5, TrackedResource(released, 1));

    CHECK_THROWS_AS(queue.retire(4, TrackedResource(released, 2)), std::logic_error);
    CHECK(queue.pending_count() == 1);
}

TEST_CASE("destroying the queue releases what is still pending in retirement order") {
    std::vector<int> released;
    {
        RetirementQueue queue;
        queue.retire(7, TrackedResource(released, 1));
        queue.retire(9, TrackedResource(released, 2));
    }

    CHECK(released == std::vector<int>{1, 2});
}
