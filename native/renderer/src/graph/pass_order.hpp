#pragma once

#include <vector>

#include "graph/render_graph.hpp"

namespace minemod::renderer {

std::vector<PassId> order_passes(const RenderGraph& graph);

}
