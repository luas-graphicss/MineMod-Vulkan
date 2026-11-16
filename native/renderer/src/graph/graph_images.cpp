#include "graph/graph_images.hpp"

#include <cstdint>
#include <optional>
#include <variant>

namespace minemod::renderer {

namespace {

BoundImage bind_image(const GraphImage& image, std::optional<std::uint32_t> slot, const TransientImages& transients) {
    if (const auto* const imported = std::get_if<ImportedImage>(&image.source)) {
        return BoundImage{imported->handle, imported->view, imported->extent, imported->aspect};
    }
    const Image& backing = transients.in_slot(slot.value());
    return BoundImage{
        backing.get(), backing.view(), backing.extent(), std::get<ImageDescription>(image.source).aspect};
}

}

GraphImages::GraphImages(const RenderGraph& graph, const GraphPlan& plan, const TransientImages& transients) {
    bound_.reserve(graph.images().size());
    for (std::uint32_t image = 0; image < graph.images().size(); ++image) {
        bound_.push_back(bind_image(graph.images()[image], plan.transients.slot_of_image.at(image), transients));
    }
}

}
