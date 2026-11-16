#pragma once

#include <cstdint>
#include <optional>
#include <span>
#include <vector>

#include "graph/render_graph.hpp"
#include "resources/image.hpp"

namespace minemod::renderer {

struct TransientSlots {
    std::vector<ImageDescription> descriptions;
    std::vector<std::optional<std::uint32_t>> slot_of_image;
};

TransientSlots assign_transient_slots(const RenderGraph& graph, std::span<const PassId> order);

}
