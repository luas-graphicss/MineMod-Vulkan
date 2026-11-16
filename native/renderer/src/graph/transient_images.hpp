#pragma once

#include <cstdint>
#include <span>
#include <vector>

#include "device/device.hpp"
#include "resources/image.hpp"

namespace minemod::renderer {

class TransientImages {
public:
    TransientImages(const Device& device, std::span<const ImageDescription> slots);

    const Image& in_slot(std::uint32_t slot) const {
        return images_.at(slot);
    }

private:
    std::vector<Image> images_;
};

}
