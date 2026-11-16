#pragma once

#include <cstddef>
#include <optional>
#include <span>

#include "draw/batch_renderer.hpp"
#include "frame/recorded_frame.hpp"
#include "graph/image_use.hpp"

namespace minemod::scene {

enum class PartEnding { hands_over, ends_frame };

struct TargetUses {
    renderer::ImageUse entry;
    std::optional<renderer::ImageUse> exit;
};

struct PartTargets {
    TargetUses color;
    TargetUses depth;
};

struct PartBatches {
    std::span<const renderer::DrawBatch> sky;
    std::span<const renderer::DrawBatch> solid;
    std::span<const renderer::DrawBatch> blended;
    std::span<const renderer::DrawBatch> hand;
    std::span<const renderer::DrawBatch> backdrop;
    std::span<const renderer::DrawBatch> interface;

    bool has_world_batches() const noexcept {
        return !sky.empty() || !solid.empty() || !blended.empty();
    }

    std::size_t size() const noexcept {
        return sky.size() + solid.size() + blended.size() + hand.size() + backdrop.size() + interface.size();
    }
};

PartTargets part_targets(const RecordedFrame& frame, PartEnding ending) noexcept;
PartBatches undrawn_batches(const RecordedFrame& frame) noexcept;

}
