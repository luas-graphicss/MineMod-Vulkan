#pragma once

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "graph/graph_images.hpp"
#include "graph/graph_plan.hpp"
#include "graph/render_graph.hpp"
#include "graph/transient_images.hpp"

namespace minemod::renderer {

class PlannedFrame {
public:
    PlannedFrame(const Device& device, RenderGraph graph);

    const GraphPlan& plan() const noexcept {
        return plan_;
    }

    void record(VkCommandBuffer commands) const;

private:
    RenderGraph graph_;
    GraphPlan plan_;
    TransientImages transients_;
    GraphImages images_;
};

}
