#include "graph/graph_plan.hpp"

#include <cstddef>
#include <cstdint>
#include <optional>
#include <variant>

#include "graph/graph_error.hpp"
#include "graph/image_state.hpp"
#include "graph/pass_order.hpp"

namespace minemod::renderer {

namespace {

class FrameImageStates {
public:
    FrameImageStates(const RenderGraph& graph, const TransientSlots& transients)
        : states_(transients.descriptions.size(), ImageState(uses::untouched)), users_(transients.descriptions.size()) {
        for (std::uint32_t image = 0; image < graph.images().size(); ++image) {
            const std::optional<std::uint32_t> slot = transients.slot_of_image[image];
            state_of_image_.push_back(slot ? *slot : states_.size());
            if (!slot) {
                states_.emplace_back(std::get<ImportedImage>(graph.images()[image].source).entry);
                users_.emplace_back(image);
            }
        }
    }

    ImageState& used_by(ImageId image) {
        const std::size_t state = state_of_image_[image.index];
        if (users_[state] != image.index) {
            states_[state].discard_contents();
            users_[state] = image.index;
        }
        return states_[state];
    }

private:
    std::vector<ImageState> states_;
    std::vector<std::optional<std::uint32_t>> users_;
    std::vector<std::size_t> state_of_image_;
};

std::vector<ImageTransition> plan_pass_transitions(
    const RenderGraph& graph, const GraphPass& pass, FrameImageStates& states) {
    std::vector<ImageTransition> transitions;
    for (const ImageAccess& access : pass.accesses) {
        ImageState& state = states.used_by(access.image);
        if (!writes_image(access.use) && !state.has_contents()) {
            throw GraphError(
                "the pass '" + pass.name + "' reads the image '" + graph.images()[access.image.index].name
                + "' before any pass writes it");
        }
        if (const std::optional<LayoutChange> change = state.advance(access.use)) {
            transitions.push_back(ImageTransition{access.image, *change});
        }
    }
    return transitions;
}

std::vector<ImageTransition> plan_closing_transitions(const RenderGraph& graph, FrameImageStates& states) {
    std::vector<ImageTransition> transitions;
    for (std::uint32_t image = 0; image < graph.images().size(); ++image) {
        const auto* const imported = std::get_if<ImportedImage>(&graph.images()[image].source);
        if (imported == nullptr || !imported->exit) {
            continue;
        }
        if (const std::optional<LayoutChange> change = states.used_by(ImageId{image}).advance(*imported->exit)) {
            transitions.push_back(ImageTransition{ImageId{image}, *change});
        }
    }
    return transitions;
}

}

GraphPlan plan_graph(const RenderGraph& graph) {
    const std::vector<PassId> order = order_passes(graph);
    GraphPlan plan{{}, {}, assign_transient_slots(graph, order)};
    FrameImageStates states(graph, plan.transients);

    plan.passes.reserve(order.size());
    for (const PassId pass : order) {
        plan.passes.push_back(PlannedPass{pass, plan_pass_transitions(graph, graph.passes()[pass.index], states)});
    }
    plan.closing_transitions = plan_closing_transitions(graph, states);
    return plan;
}

}
