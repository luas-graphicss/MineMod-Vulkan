#include <array>
#include <chrono>
#include <cstdint>
#include <stdexcept>
#include <vector>

#include <doctest.h>

#include "metrics/frame_metrics.hpp"

namespace {

using namespace minemod::renderer;
using namespace std::chrono_literals;

FrameSample timed_frame(std::uint64_t frame, std::chrono::nanoseconds cpu_time) {
    return FrameSample{frame, cpu_time, {}, {}, {}};
}

}

TEST_CASE("the history keeps only the most recent frames") {
    FrameMetrics metrics(2);
    metrics.record(timed_frame(1, 4ms));
    metrics.record(timed_frame(2, 6ms));
    metrics.record(timed_frame(3, 10ms));

    const MetricsSummary summary = metrics.summarize();

    CHECK(summary.frame_count == 2);
    CHECK(summary.mean_frame_time == 8ms);
    CHECK(summary.worst_frame_time == 10ms);
}

TEST_CASE("pass times and draws accumulate by pass name") {
    FrameMetrics metrics(8);
    metrics.record(FrameSample{1, 5ms, {{"terrain", 2ms, 10}, {"gui", 1ms, 3}}, {}, {}});
    metrics.record(FrameSample{2, 5ms, {{"terrain", 3ms, 14}}, {}, {}});

    const MetricsSummary summary = metrics.summarize();

    REQUIRE(summary.pass_totals.size() == 2);
    CHECK(summary.pass_totals[0].name == "terrain");
    CHECK(summary.pass_totals[0].cpu_time == 5ms);
    CHECK(summary.pass_totals[0].draw_count == 24);
    CHECK(summary.pass_totals[1].draw_count == 3);
}

TEST_CASE("resource and queue peaks keep the largest value seen") {
    FrameMetrics metrics(8);
    metrics.record(FrameSample{1, 5ms, {}, {{ResourceKind::texture, 4096, 2}}, {{"meshing", 3, 8}}});
    metrics.record(FrameSample{2, 5ms, {}, {{ResourceKind::texture, 1024, 1}}, {{"meshing", 7, 8}}});

    const MetricsSummary summary = metrics.summarize();

    REQUIRE(summary.resource_peaks.size() == 1);
    CHECK(summary.resource_peaks[0].bytes == 4096);
    CHECK(summary.resource_peaks[0].count == 2);
    REQUIRE(summary.queue_peaks.size() == 1);
    CHECK(summary.queue_peaks[0].depth == 7);
}

TEST_CASE("a frame reports the resource kinds that exceed their budget") {
    const FrameSample sample{
        1, 5ms, {}, {{ResourceKind::texture, 4096, 2}, {ResourceKind::buffer, 512, 4}}, {},
    };
    const std::array budgets{
        ResourceBudget{ResourceKind::texture, 2048},
        ResourceBudget{ResourceKind::buffer, 1024},
        ResourceBudget{ResourceKind::pipeline, 1},
    };

    CHECK(exceeded_budgets(sample, budgets) == std::vector<ResourceKind>{ResourceKind::texture});
}

TEST_CASE("the summary text lists frames, passes, resources and queues") {
    FrameMetrics metrics(4);
    metrics.record(FrameSample{
        1, 4ms, {{"terrain", 1500us, 12}}, {{ResourceKind::texture, 4096, 2}}, {{"meshing", 3, 8}},
    });

    CHECK(
        describe(metrics.summarize())
        == "frames: 1\n"
           "frame time: mean 4000 us, worst 4000 us\n"
           "pass terrain: 1500 us, 12 draw(s)\n"
           "resource texture: peak 4096 byte(s) in 2 object(s)\n"
           "queue meshing: peak 3 of 8\n");
}

TEST_CASE("an empty history summarizes to zero frames and a history without frames is rejected") {
    CHECK(FrameMetrics(4).summarize().frame_count == 0);
    CHECK_THROWS_AS(FrameMetrics(0), std::invalid_argument);
}
