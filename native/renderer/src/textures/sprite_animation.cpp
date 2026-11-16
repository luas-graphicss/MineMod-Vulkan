#include "textures/sprite_animation.hpp"

#include <cstddef>
#include <stdexcept>
#include <utility>

namespace minemod::renderer {

SpriteAnimation::SpriteAnimation(std::vector<AnimationFrame> frames, FrameBlending blending)
    : frames_(std::move(frames)), blending_(blending) {
    if (frames_.empty()) {
        throw std::invalid_argument("a sprite animation needs at least one frame");
    }
    for (const AnimationFrame& frame : frames_) {
        if (frame.duration_ticks == 0) {
            throw std::invalid_argument("an animation frame must last at least one tick");
        }
        cycle_ticks_ += frame.duration_ticks;
    }
}

AnimationPose SpriteAnimation::pose_at(std::uint64_t tick) const {
    std::uint64_t ticks_into_frame = tick % cycle_ticks_;
    std::size_t frame = 0;
    while (ticks_into_frame >= frames_[frame].duration_ticks) {
        ticks_into_frame -= frames_[frame].duration_ticks;
        ++frame;
    }

    const AnimationFrame& current = frames_[frame];
    const AnimationFrame& next = frames_[(frame + 1) % frames_.size()];
    if (blending_ == FrameBlending::stepped) {
        return AnimationPose{current.sprite_frame, next.sprite_frame, 0.0f};
    }
    const float blend = static_cast<float>(ticks_into_frame) / static_cast<float>(current.duration_ticks);
    return AnimationPose{current.sprite_frame, next.sprite_frame, blend};
}

}
