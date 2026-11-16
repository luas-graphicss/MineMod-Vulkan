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
    record_passes(commands, nullptr);
}

std::vector<PassTiming> PlannedFrame::record_timed(VkCommandBuffer commands) const {
    std::vector<PassTiming> timings;
    timings.reserve(plan_.passes.size());
    record_passes(commands, &timings);
    return timings;
}

void PlannedFrame::record_passes(VkCommandBuffer commands, std::vector<PassTiming>* timings) const {
    for (const PlannedPass& planned : plan_.passes) {
        const GraphPass& pass = graph_.passes()[planned.pass.index];
        const auto started = std::chrono::steady_clock::now();
        record_transitions(commands, planned.transitions, images_);
        pass.record(commands, images_);
        if (timings != nullptr) {
            timings->push_back(PassTiming{pass.name, std::chrono::steady_clock::now() - started});
        }
    }
    record_transitions(commands, plan_.closing_transitions, images_);
}

}
