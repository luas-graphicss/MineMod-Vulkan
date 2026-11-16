#include <vector>

#include <doctest.h>

#include "lifetime/retirement_queue.hpp"
#include "reload/resource_generations.hpp"
#include "support/tracked_resource.hpp"

using namespace minemod::renderer;
using namespace minemod::renderer::test_support;

TEST_CASE("a staged generation replaces the current one only when it is published") {
    std::vector<int> released;
    RetirementQueue retirements;
    ResourceGenerations<TrackedResource> generations(TrackedResource(released, 1));
    generations.stage(TrackedResource(released, 2));

    CHECK(generations.current().id() == 1);
    CHECK(generations.publish(10, retirements));
    CHECK(generations.current().id() == 2);
    CHECK(generations.generation() == 1);
}

TEST_CASE("the replaced generation is released after the last frame that used it") {
    std::vector<int> released;
    RetirementQueue retirements;
    ResourceGenerations<TrackedResource> generations(TrackedResource(released, 1));
    generations.stage(TrackedResource(released, 2));
    generations.publish(10, retirements);

    retirements.release_through(9);
    CHECK(released.empty());
    retirements.release_through(10);
    CHECK(released == std::vector<int>{1});
}

TEST_CASE("a reload that staged nothing keeps the generation in use") {
    std::vector<int> released;
    RetirementQueue retirements;
    ResourceGenerations<TrackedResource> generations(TrackedResource(released, 1));

    CHECK_FALSE(generations.publish(3, retirements));
    CHECK(generations.current().id() == 1);
    CHECK(generations.generation() == 0);
    CHECK(retirements.pending_count() == 0);
}

TEST_CASE("a newer candidate replaces one that was never published") {
    std::vector<int> released;
    RetirementQueue retirements;
    ResourceGenerations<TrackedResource> generations(TrackedResource(released, 1));
    generations.stage(TrackedResource(released, 2));

    generations.stage(TrackedResource(released, 3));

    CHECK(released == std::vector<int>{2});
    CHECK(generations.publish(5, retirements));
    CHECK(generations.current().id() == 3);
    CHECK(generations.generation() == 1);
}
