#pragma once

#include <span>
#include <string>

#include <vulkan/vulkan.h>

#include "device/instance.hpp"

namespace minemod::scene {

class SceneHost {
public:
    SceneHost(renderer::Validation validation, std::span<const char* const> window_system_extensions)
        : instance_(validation, window_system_extensions) {}

    VkInstance instance() const noexcept {
        return instance_.get();
    }

    std::string validation_text() const;

private:
    renderer::Instance instance_;
};

}
