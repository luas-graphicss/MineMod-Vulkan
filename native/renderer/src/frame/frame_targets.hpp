#pragma once

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "resources/image.hpp"

namespace minemod::renderer {

inline constexpr VkFormat frame_color_format = VK_FORMAT_R8G8B8A8_UNORM;
inline constexpr VkFormat frame_depth_format = VK_FORMAT_D32_SFLOAT;

class FrameTargets {
public:
    FrameTargets(const Device& device, VkExtent2D extent);

    const Image& color() const noexcept {
        return color_;
    }

    const Image& depth() const noexcept {
        return depth_;
    }

    const Image& scratch() const noexcept {
        return scratch_;
    }

    VkExtent2D extent() const noexcept {
        return color_.extent();
    }

private:
    Image color_;
    Image depth_;
    Image scratch_;
};

}
