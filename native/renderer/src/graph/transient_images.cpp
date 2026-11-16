#include "graph/transient_images.hpp"

namespace minemod::renderer {

TransientImages::TransientImages(const Device& device, std::span<const ImageDescription> slots) {
    images_.reserve(slots.size());
    for (const ImageDescription& description : slots) {
        images_.emplace_back(device, description);
    }
}

}
