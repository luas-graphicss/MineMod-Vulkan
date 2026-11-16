#pragma once

#include <chrono>
#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include "frame/frame_slot.hpp"
#include "metrics/frame_metrics.hpp"
#include "minemod/scene/scene_session.h"

namespace minemod::scene {

struct FrameUsage {
    std::uint64_t frame;
    std::uint64_t texture_bytes;
    std::uint32_t textures;
    std::uint64_t staging_bytes;
    std::uint32_t pipelines;
    std::size_t frames_in_flight;
    std::size_t max_frames_in_flight;
};

struct SessionState {
    std::size_t retired_resources;
    std::uint64_t resource_generation;
};

class SessionMetrics {
public:
    SessionMetrics();

    void start_frame();
    void finish_frame(const FrameUsage& usage, std::vector<renderer::PassSample> passes);

    minemod_scene_metrics numbers(const SessionState& state) const;
    std::string report(const SessionState& state, const SceneLimits& limits) const;

private:
    renderer::FrameMetrics history_;
    std::optional<std::chrono::steady_clock::time_point> frame_started_;
};

}
