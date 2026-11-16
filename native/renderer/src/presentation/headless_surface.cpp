#include "presentation/headless_surface.hpp"

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

HeadlessSurface::HeadlessSurface(VkInstance instance) : instance_(instance) {
    const VkHeadlessSurfaceCreateInfoEXT create_info{.sType = VK_STRUCTURE_TYPE_HEADLESS_SURFACE_CREATE_INFO_EXT};
    require_success(
        vkCreateHeadlessSurfaceEXT(instance_, &create_info, nullptr, &surface_), "creating the headless surface");
}

HeadlessSurface::~HeadlessSurface() {
    vkDestroySurfaceKHR(instance_, surface_, nullptr);
}

}
