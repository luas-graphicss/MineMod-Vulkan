#pragma once

#include <chrono>
#include <cstddef>
#include <cstdint>
#include <deque>
#include <span>
#include <string>
#include <vector>

namespace minemod::renderer {

enum class ResourceKind { buffer, texture, pipeline, descriptor_set, upload_staging };

struct PassSample {
    std::string name;
    std::chrono::nanoseconds cpu_time;
    std::uint32_t draw_count;
};

struct ResourceSample {
    ResourceKind kind;
    std::uint64_t bytes;
    std::uint32_t count;
};

struct QueueSample {
    std::string name;
    std::size_t depth;
    std::size_t capacity;
};

struct FrameSample {
    std::uint64_t frame;
    std::chrono::nanoseconds cpu_time;
    std::vector<PassSample> passes;
    std::vector<ResourceSample> resources;
    std::vector<QueueSample> queues;
};

struct ResourceBudget {
    ResourceKind kind;
    std::uint64_t max_bytes;
};

struct MetricsSummary {
    std::size_t frame_count;
    std::chrono::nanoseconds mean_frame_time;
    std::chrono::nanoseconds worst_frame_time;
    std::vector<PassSample> pass_totals;
    std::vector<ResourceSample> resource_peaks;
    std::vector<QueueSample> queue_peaks;
};

class FrameMetrics {
public:
    explicit FrameMetrics(std::size_t history_frames);

    void record(FrameSample sample);
    MetricsSummary summarize() const;

private:
    std::size_t history_frames_;
    std::deque<FrameSample> history_;
};

std::vector<ResourceKind> exceeded_budgets(const FrameSample& sample, std::span<const ResourceBudget> budgets);

std::string describe(const MetricsSummary& summary);

}
