#pragma once

#include <chrono>
#include <string_view>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "graph/graph_images.hpp"
#include "graph/graph_plan.hpp"
#include "graph/render_graph.hpp"
#include "graph/transient_images.hpp"

namespace minemod::renderer {

struct PassTiming {
    std::string_view name;
    std::chrono::nanoseconds cpu_time;
};

class PlannedFrame {
public:
    PlannedFrame(const Device& device, RenderGraph graph);

    const GraphPlan& plan() const noexcept {
        return plan_;
    }

    void record(VkCommandBuffer commands) const;
    std::vector<PassTiming> record_timed(VkCommandBuffer commands) const;

private:
    void record_passes(VkCommandBuffer commands, std::vector<PassTiming>* timings) const;

    RenderGraph graph_;
    GraphPlan plan_;
    TransientImages transients_;
    GraphImages images_;
};

}
