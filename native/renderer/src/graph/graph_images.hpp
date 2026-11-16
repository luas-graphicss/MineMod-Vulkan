#pragma once

#include <vector>

#include <vulkan/vulkan.h>

#include "graph/graph_plan.hpp"
#include "graph/render_graph.hpp"
#include "graph/transient_images.hpp"

namespace minemod::renderer {

struct BoundImage {
    VkImage handle;
    VkImageView view;
    VkExtent2D extent;
    VkImageAspectFlags aspect;
};

class GraphImages {
public:
    GraphImages(const RenderGraph& graph, const GraphPlan& plan, const TransientImages& transients);

    const BoundImage& at(ImageId image) const {
        return bound_.at(image.index);
    }

private:
    std::vector<BoundImage> bound_;
};

}
