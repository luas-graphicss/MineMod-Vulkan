#include "graph/render_graph.hpp"

#include <algorithm>
#include <cstddef>
#include <utility>

#include "graph/graph_error.hpp"

namespace minemod::renderer {

namespace {

template <typename Declared>
std::uint32_t last_index(const std::vector<Declared>& declared) {
    return static_cast<std::uint32_t>(declared.size() - 1);
}

void require_usable_access(
    std::span<const GraphImage> images,
    const std::string& pass,
    const ImageAccess& access,
    std::span<const ImageAccess> earlier_accesses) {
    if (access.image.index >= images.size()) {
        throw GraphError("the pass '" + pass + "' uses an image this graph did not declare");
    }
    const std::string& image = images[access.image.index].name;
    if (access.use.layout == VK_IMAGE_LAYOUT_UNDEFINED) {
        throw GraphError("the pass '" + pass + "' uses the image '" + image + "' without a layout");
    }
    const bool is_repeated = std::ranges::any_of(
        earlier_accesses, [&](const ImageAccess& earlier) { return earlier.image == access.image; });
    if (is_repeated) {
        throw GraphError("the pass '" + pass + "' uses the image '" + image + "' more than once");
    }
}

}

ImageId RenderGraph::create_image(std::string name, const ImageDescription& description) {
    images_.push_back(GraphImage{std::move(name), description});
    return ImageId{last_index(images_)};
}

ImageId RenderGraph::import_image(std::string name, const ImportedImage& image) {
    if (image.exit && image.exit->layout == VK_IMAGE_LAYOUT_UNDEFINED) {
        throw GraphError("the imported image '" + name + "' cannot leave the graph without a layout");
    }
    images_.push_back(GraphImage{std::move(name), image});
    return ImageId{last_index(images_)};
}

PassId RenderGraph::add_pass(std::string name, std::vector<ImageAccess> accesses, RecordPass record) {
    if (!record) {
        throw GraphError("the pass '" + name + "' has nothing to record");
    }
    for (std::size_t declared = 0; declared < accesses.size(); ++declared) {
        require_usable_access(images_, name, accesses[declared], std::span(accesses).first(declared));
    }
    passes_.push_back(GraphPass{std::move(name), std::move(accesses), {}, std::move(record)});
    return PassId{last_index(passes_)};
}

void RenderGraph::add_dependency(PassId pass, PassId dependency) {
    const GraphPass& dependent = declared_pass(pass);
    declared_pass(dependency);
    if (pass == dependency) {
        throw GraphError("the pass '" + dependent.name + "' cannot depend on itself");
    }
    passes_[pass.index].dependencies.push_back(dependency);
}

const GraphPass& RenderGraph::declared_pass(PassId pass) const {
    if (pass.index >= passes_.size()) {
        throw GraphError("a dependency names a pass this graph did not declare");
    }
    return passes_[pass.index];
}

}
