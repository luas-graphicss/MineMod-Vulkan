#pragma once

#include <string>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/instance.hpp"
#include "presentation/headless_surface.hpp"

namespace minemod::renderer::test_support {

class PresentingDevice {
public:
    PresentingDevice();

    const Device& device() const noexcept {
        return device_;
    }

    VkInstance instance() const noexcept {
        return instance_.get();
    }

    VkSurfaceKHR surface() const noexcept {
        return surface_.get();
    }

    std::string validation_text() const;

private:
    Instance instance_;
    HeadlessSurface surface_;
    Device device_;
};

}
