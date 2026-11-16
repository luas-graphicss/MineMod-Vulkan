#pragma once

#include <cstdint>
#include <vector>

namespace minemod::renderer {

enum class FrameBlending { stepped, interpolated };

struct AnimationFrame {
    std::uint32_t sprite_frame;
    std::uint32_t duration_ticks;
};

struct AnimationPose {
    std::uint32_t current_frame;
    std::uint32_t next_frame;
    float blend_toward_next;

    friend bool operator==(const AnimationPose&, const AnimationPose&) = default;
};

class SpriteAnimation {
public:
    SpriteAnimation(std::vector<AnimationFrame> frames, FrameBlending blending);

    AnimationPose pose_at(std::uint64_t tick) const;

private:
    std::vector<AnimationFrame> frames_;
    FrameBlending blending_;
    std::uint64_t cycle_ticks_ = 0;
};

}
