#include "graph/planned_frame.hpp"

#include <span>
#include <utility>

#include "resources/image_layout.hpp"

namespace minemod::renderer {

namespace {

void record_transitions(
    VkCommandBuffer commands, std::span<const ImageTransition> transitions, const GraphImages& images) {
    for (const ImageTransition& transition : transitions) {
        const BoundImage& image = images.at(transition.image);
        change_layout(commands, image.handle, image.aspect, transition.change);
    }
}

}

PlannedFrame::PlannedFrame(const Device& device, RenderGraph graph)
    : graph_(std::move(graph)),
      plan_(plan_graph(graph_)),
      transients_(device, plan_.transients.descriptions),
      images_(graph_, plan_, transients_) {}

void PlannedFrame::record(VkCommandBuffer commands) const {
    for (const PlannedPass& planned : plan_.passes) {
        record_transitions(commands, planned.transitions, images_);
        graph_.passes()[planned.pass.index].record(commands, images_);
    }
    record_transitions(commands, plan_.closing_transitions, images_);
}

}
