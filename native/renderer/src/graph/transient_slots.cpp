#include "graph/transient_slots.hpp"

#include <algorithm>
#include <cstddef>
#include <utility>
#include <variant>

#include "graph/graph_error.hpp"

namespace minemod::renderer {

namespace {

struct Lifetime {
    std::size_t first_use;
    std::size_t last_use;
};

std::vector<Lifetime> measure_lifetimes(const RenderGraph& graph, std::span<const PassId> order) {
    std::vector<std::optional<Lifetime>> measured(graph.images().size());
    for (std::size_t position = 0; position < order.size(); ++position) {
        for (const ImageAccess& access : graph.passes()[order[position].index].accesses) {
            std::optional<Lifetime>& lifetime = measured[access.image.index];
            lifetime = Lifetime{lifetime ? lifetime->first_use : position, position};
        }
    }

    std::vector<Lifetime> lifetimes;
    lifetimes.reserve(measured.size());
    for (std::size_t image = 0; image < measured.size(); ++image) {
        if (!measured[image]) {
            throw GraphError("the image '" + graph.images()[image].name + "' is not used by any pass");
        }
        lifetimes.push_back(*measured[image]);
    }
    return lifetimes;
}

std::vector<std::uint32_t> transient_images_by_first_use(const RenderGraph& graph, std::span<const Lifetime> lifetimes) {
    std::vector<std::uint32_t> images;
    for (std::uint32_t image = 0; image < graph.images().size(); ++image) {
        if (std::holds_alternative<ImageDescription>(graph.images()[image].source)) {
            images.push_back(image);
        }
    }
    std::ranges::stable_sort(images, {}, [&](std::uint32_t image) { return lifetimes[image].first_use; });
    return images;
}

bool is_same_description(const ImageDescription& left, const ImageDescription& right) {
    return left.extent.width == right.extent.width && left.extent.height == right.extent.height
        && left.format == right.format && left.usage == right.usage && left.aspect == right.aspect
        && left.mip_levels == right.mip_levels;
}

class SlotClaims {
public:
    std::uint32_t claim(const ImageDescription& description, const Lifetime& lifetime) {
        for (std::uint32_t slot = 0; slot < descriptions_.size(); ++slot) {
            if (is_same_description(descriptions_[slot], description) && last_uses_[slot] < lifetime.first_use) {
                last_uses_[slot] = lifetime.last_use;
                return slot;
            }
        }
        descriptions_.push_back(description);
        last_uses_.push_back(lifetime.last_use);
        return static_cast<std::uint32_t>(descriptions_.size() - 1);
    }

    std::vector<ImageDescription> release_descriptions() {
        return std::move(descriptions_);
    }

private:
    std::vector<ImageDescription> descriptions_;
    std::vector<std::size_t> last_uses_;
};

}

TransientSlots assign_transient_slots(const RenderGraph& graph, std::span<const PassId> order) {
    const std::vector<Lifetime> lifetimes = measure_lifetimes(graph, order);

    SlotClaims claims;
    std::vector<std::optional<std::uint32_t>> slot_of_image(graph.images().size());
    for (const std::uint32_t image : transient_images_by_first_use(graph, lifetimes)) {
        const auto& description = std::get<ImageDescription>(graph.images()[image].source);
        slot_of_image[image] = claims.claim(description, lifetimes[image]);
    }
    return TransientSlots{claims.release_descriptions(), std::move(slot_of_image)};
}

}
