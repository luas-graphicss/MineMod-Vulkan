#include "metrics/frame_metrics.hpp"

#include <algorithm>
#include <sstream>
#include <stdexcept>
#include <string_view>
#include <utility>

namespace minemod::renderer {

namespace {

void add_pass_totals(std::vector<PassSample>& totals, std::span<const PassSample> passes) {
    for (const PassSample& pass : passes) {
        const auto total = std::ranges::find(totals, pass.name, &PassSample::name);
        if (total == totals.end()) {
            totals.push_back(pass);
            continue;
        }
        total->cpu_time += pass.cpu_time;
        total->draw_count += pass.draw_count;
    }
}

void raise_resource_peaks(std::vector<ResourceSample>& peaks, std::span<const ResourceSample> resources) {
    for (const ResourceSample& resource : resources) {
        const auto peak = std::ranges::find(peaks, resource.kind, &ResourceSample::kind);
        if (peak == peaks.end()) {
            peaks.push_back(resource);
            continue;
        }
        peak->bytes = std::max(peak->bytes, resource.bytes);
        peak->count = std::max(peak->count, resource.count);
    }
}

void raise_queue_peaks(std::vector<QueueSample>& peaks, std::span<const QueueSample> queues) {
    for (const QueueSample& queue : queues) {
        const auto peak = std::ranges::find(peaks, queue.name, &QueueSample::name);
        if (peak == peaks.end()) {
            peaks.push_back(queue);
            continue;
        }
        peak->depth = std::max(peak->depth, queue.depth);
        peak->capacity = queue.capacity;
    }
}

std::string_view resource_kind_name(ResourceKind kind) {
    switch (kind) {
        case ResourceKind::buffer:
            return "buffer";
        case ResourceKind::texture:
            return "texture";
        case ResourceKind::pipeline:
            return "pipeline";
        case ResourceKind::descriptor_set:
            return "descriptor set";
        case ResourceKind::upload_staging:
            return "upload staging";
    }
    throw std::invalid_argument("the resource kind is not part of the metrics");
}

long long microseconds(std::chrono::nanoseconds duration) {
    return std::chrono::duration_cast<std::chrono::microseconds>(duration).count();
}

}

FrameMetrics::FrameMetrics(std::size_t history_frames) : history_frames_(history_frames) {
    if (history_frames == 0) {
        throw std::invalid_argument("the metrics history keeps at least one frame");
    }
}

void FrameMetrics::record(FrameSample sample) {
    if (history_.size() == history_frames_) {
        history_.pop_front();
    }
    history_.push_back(std::move(sample));
}

MetricsSummary FrameMetrics::summarize() const {
    MetricsSummary summary{};
    summary.frame_count = history_.size();
    if (history_.empty()) {
        return summary;
    }

    std::chrono::nanoseconds total_frame_time{0};
    for (const FrameSample& sample : history_) {
        total_frame_time += sample.cpu_time;
        summary.worst_frame_time = std::max(summary.worst_frame_time, sample.cpu_time);
        add_pass_totals(summary.pass_totals, sample.passes);
        raise_resource_peaks(summary.resource_peaks, sample.resources);
        raise_queue_peaks(summary.queue_peaks, sample.queues);
    }
    summary.mean_frame_time = total_frame_time / static_cast<std::chrono::nanoseconds::rep>(history_.size());
    return summary;
}

std::vector<ResourceKind> exceeded_budgets(const FrameSample& sample, std::span<const ResourceBudget> budgets) {
    std::vector<ResourceKind> exceeded;
    for (const ResourceBudget& budget : budgets) {
        const auto usage = std::ranges::find(sample.resources, budget.kind, &ResourceSample::kind);
        if (usage != sample.resources.end() && usage->bytes > budget.max_bytes) {
            exceeded.push_back(budget.kind);
        }
    }
    return exceeded;
}

std::string describe(const MetricsSummary& summary) {
    std::ostringstream text;
    text << "frames: " << summary.frame_count << '\n';
    text << "frame time: mean " << microseconds(summary.mean_frame_time) << " us, worst "
         << microseconds(summary.worst_frame_time) << " us\n";
    for (const PassSample& pass : summary.pass_totals) {
        text << "pass " << pass.name << ": " << microseconds(pass.cpu_time) << " us, " << pass.draw_count
             << " draw(s)\n";
    }
    for (const ResourceSample& resource : summary.resource_peaks) {
        text << "resource " << resource_kind_name(resource.kind) << ": peak " << resource.bytes << " byte(s) in "
             << resource.count << " object(s)\n";
    }
    for (const QueueSample& queue : summary.queue_peaks) {
        text << "queue " << queue.name << ": peak " << queue.depth << " of " << queue.capacity << '\n';
    }
    return text.str();
}

}
