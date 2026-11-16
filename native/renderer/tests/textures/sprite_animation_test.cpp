#include <stdexcept>

#include <doctest.h>

#include "textures/sprite_animation.hpp"

using namespace minemod::renderer;

TEST_CASE("a stepped animation holds each frame for its duration and starts over") {
    const SpriteAnimation animation({{0, 2}, {3, 1}}, FrameBlending::stepped);

    CHECK(animation.pose_at(0) == AnimationPose{0, 3, 0.0f});
    CHECK(animation.pose_at(1) == AnimationPose{0, 3, 0.0f});
    CHECK(animation.pose_at(2) == AnimationPose{3, 0, 0.0f});
    CHECK(animation.pose_at(3) == AnimationPose{0, 3, 0.0f});
}

TEST_CASE("an interpolated animation blends toward the next frame and wraps to the first") {
    const SpriteAnimation animation({{0, 4}, {1, 2}}, FrameBlending::interpolated);

    CHECK(animation.pose_at(1) == AnimationPose{0, 1, 0.25f});
    CHECK(animation.pose_at(5) == AnimationPose{1, 0, 0.5f});
    CHECK(animation.pose_at(6) == AnimationPose{0, 1, 0.0f});
}

TEST_CASE("animations without frames or with frames that last no tick are rejected") {
    CHECK_THROWS_AS(SpriteAnimation({}, FrameBlending::stepped), std::invalid_argument);
    CHECK_THROWS_AS(SpriteAnimation({{0, 0}}, FrameBlending::stepped), std::invalid_argument);
}
