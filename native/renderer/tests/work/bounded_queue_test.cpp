#include <stdexcept>

#include <doctest.h>

#include "work/bounded_queue.hpp"

using namespace minemod::renderer;

TEST_CASE("jobs leave the queue in the order they entered") {
    BoundedQueue<int> queue(3);
    queue.try_push(7);
    queue.try_push(8);

    CHECK(queue.try_pop() == 7);
    CHECK(queue.try_pop() == 8);
    CHECK_FALSE(queue.try_pop().has_value());
}

TEST_CASE("a full queue refuses another job and keeps the ones it holds") {
    BoundedQueue<int> queue(2);
    REQUIRE(queue.try_push(1));
    REQUIRE(queue.try_push(2));

    CHECK_FALSE(queue.try_push(3));
    CHECK(queue.size() == 2);
    CHECK(queue.try_pop() == 1);
}

TEST_CASE("discarding stale jobs makes room in a full queue") {
    BoundedQueue<int> queue(3);
    queue.try_push(1);
    queue.try_push(2);
    queue.try_push(3);

    CHECK(queue.discard_if([](int job) { return job != 2; }) == 2);
    CHECK(queue.try_push(4));
    CHECK(queue.try_pop() == 2);
    CHECK(queue.try_pop() == 4);
}

TEST_CASE("a queue without room for a job is rejected") {
    CHECK_THROWS_AS(BoundedQueue<int>(0), std::invalid_argument);
}
