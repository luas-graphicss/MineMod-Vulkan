#include "presentation/window_surface.hpp"

#include <stdexcept>

namespace minemod::renderer {

WindowSurface::WindowSurface(VkInstance instance, VkSurfaceKHR created_by_window_system)
    : instance_(instance), surface_(created_by_window_system) {
    if (surface_ == VK_NULL_HANDLE) {
        throw std::invalid_argument("the window system handed over no surface");
    }
}

WindowSurface::~WindowSurface() {
    vkDestroySurfaceKHR(instance_, surface_, nullptr);
}

}
