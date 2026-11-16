#include "metrics/session_metrics.hpp"

#include <algorithm>
#include <utility>

namespace minemod::scene {

namespace {

constexpr std::size_t measured_frames = 240;
constexpr std::uint32_t staging_arenas = 2;

std::uint64_t nanoseconds(std::chrono::nanoseconds duration) noexcept {
    return static_cast<std::uint64_t>(std::max<std::chrono::nanoseconds::rep>(duration.count(), 0));
}

renderer::ResourceSample peak_of(const renderer::MetricsSummary& summary, renderer::ResourceKind kind) noexcept {
    const auto peak = std::ranges::find(summary.resource_peaks, kind, &renderer::ResourceSample::kind);
    return peak == summary.resource_peaks.end() ? renderer::ResourceSample{kind, 0, 0} : *peak;
}

}

SessionMetrics::SessionMetrics() : history_(measured_frames) {}

void SessionMetrics::start_frame() {
    frame_started_ = std::chrono::steady_clock::now();
}

void SessionMetrics::finish_frame(const FrameUsage& usage, std::vector<renderer::PassSample> passes) {
    const auto finished = std::chrono::steady_clock::now();
    history_.record(renderer::FrameSample{
        usage.frame,
        finished - frame_started_.value_or(finished),
        std::move(passes),
        {
            {renderer::ResourceKind::texture, usage.texture_bytes, usage.textures},
            {renderer::ResourceKind::upload_staging, usage.staging_bytes, staging_arenas},
            {renderer::ResourceKind::pipeline, 0, usage.pipelines},
        },
        {{"frames in flight", usage.frames_in_flight, usage.max_frames_in_flight}},
    });
    frame_started_.reset();
}

minemod_scene_metrics SessionMetrics::numbers(const SessionState& state) const {
    const renderer::MetricsSummary summary = history_.summarize();
    return minemod_scene_metrics{
        .frames_measured = summary.frame_count,
        .mean_frame_nanoseconds = nanoseconds(summary.mean_frame_time),
        .worst_frame_nanoseconds = nanoseconds(summary.worst_frame_time),
        .peak_texture_bytes = peak_of(summary, renderer::ResourceKind::texture).bytes,
        .peak_staging_bytes = peak_of(summary, renderer::ResourceKind::upload_staging).bytes,
        .peak_pipelines = peak_of(summary, renderer::ResourceKind::pipeline).count,
        .retired_resources = state.retired_resources,
        .resource_generation = state.resource_generation,
    };
}

std::string SessionMetrics::report(const SessionState& state, const SceneLimits& limits) const {
    std::string text = renderer::describe(history_.summarize());
    text += "limit upload staging: " + std::to_string(limits.upload_bytes_per_frame) + " byte(s) per frame\n";
    text += "limit geometry staging: " + std::to_string(limits.geometry_bytes_per_frame) + " byte(s) per frame\n";
    text += "limit textures: " + std::to_string(limits.max_textures) + '\n';
    text += "limit sections: " + std::to_string(limits.max_sections) + '\n';
    text += "retired resources: " + std::to_string(state.retired_resources) + " waiting\n";
    text += "resource generation: " + std::to_string(state.resource_generation) + '\n';
    return text;
}

}
