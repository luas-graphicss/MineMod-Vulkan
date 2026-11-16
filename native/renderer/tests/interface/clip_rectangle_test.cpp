#include <cstdint>

#include <doctest.h>

#include "interface/clip_rectangle.hpp"

namespace {

using namespace minemod::renderer;

constexpr VkExtent2D frame{100, 80};

void check_rectangle(const VkRect2D& clip, const VkOffset2D& offset, const VkExtent2D& extent) {
    CHECK(clip.offset.x == offset.x);
    CHECK(clip.offset.y == offset.y);
    CHECK(clip.extent.width == extent.width);
    CHECK(clip.extent.height == extent.height);
}

}

TEST_CASE("a rectangle measured from the lower left corner is placed from the top of the frame") {
    check_rectangle(clip_rectangle({10, 20, 30, 40}, frame), {10, 20}, {30, 40});
    check_rectangle(clip_rectangle({0, 0, 100, 16}, frame), {0, 64}, {100, 16});
}

TEST_CASE("a rectangle that leaves the frame is cut at its edges") {
    check_rectangle(clip_rectangle({-10, -10, 50, 50}, frame), {0, 40}, {40, 40});
    check_rectangle(clip_rectangle({90, 70, 50, 50}, frame), {90, 0}, {10, 10});
}

TEST_CASE("a rectangle outside the frame or without a size clips everything") {
    CHECK(clip_rectangle({200, 0, 10, 10}, frame).extent.width == 0);
    CHECK(clip_rectangle({0, 200, 10, 10}, frame).extent.height == 0);
    CHECK(clip_rectangle({0, 0, -5, 10}, frame).extent.width == 0);
    CHECK(clip_rectangle({0, 0, 10, -5}, frame).extent.height == 0);
}

TEST_CASE("the largest rectangle the interface can ask for stays inside the frame") {
    constexpr std::int32_t largest = 2147483647;

    check_rectangle(clip_rectangle({-largest, -largest, largest, largest}, frame), {0, 80}, {0, 0});
    check_rectangle(clip_rectangle({0, 0, largest, largest}, frame), {0, 0}, {100, 80});
}
