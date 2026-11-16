#pragma once

#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "resources/image.hpp"

namespace minemod::renderer {

struct Pixel {
    std::uint8_t red;
    std::uint8_t green;
    std::uint8_t blue;
    std::uint8_t alpha;

    friend bool operator==(const Pixel&, const Pixel&) = default;
};

class FramePixels {
public:
    FramePixels(VkExtent2D extent, std::vector<std::byte> rgba);

    VkExtent2D extent() const noexcept {
        return extent_;
    }

    std::span<const std::byte> rgba() const noexcept {
        return rgba_;
    }

    Pixel at(std::uint32_t x, std::uint32_t y) const;

private:
    VkExtent2D extent_;
    std::vector<std::byte> rgba_;
};

FramePixels read_back_color(const Device& device, const Image& transfer_source);

}
