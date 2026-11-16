#pragma once

#include <vector>

#include "graph/render_graph.hpp"
#include "graph/transient_slots.hpp"
#include "resources/image_layout.hpp"

namespace minemod::renderer {

struct ImageTransition {
    ImageId image;
    LayoutChange change;
};

struct PlannedPass {
    PassId pass;
    std::vector<ImageTransition> transitions;
};

struct GraphPlan {
    std::vector<PlannedPass> passes;
    std::vector<ImageTransition> closing_transitions;
    TransientSlots transients;
};

GraphPlan plan_graph(const RenderGraph& graph);

}
