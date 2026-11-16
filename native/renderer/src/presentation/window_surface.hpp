#pragma once

#include <vulkan/vulkan.h>

namespace minemod::renderer {

class WindowSurface {
public:
    WindowSurface(VkInstance instance, VkSurfaceKHR created_by_window_system);
    ~WindowSurface();

    WindowSurface(const WindowSurface&) = delete;
    WindowSurface& operator=(const WindowSurface&) = delete;

    VkSurfaceKHR get() const noexcept {
        return surface_;
    }

private:
    VkInstance instance_;
    VkSurfaceKHR surface_;
};

}
