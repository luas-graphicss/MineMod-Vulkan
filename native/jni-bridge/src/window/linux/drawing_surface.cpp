#include "window/drawing_surface.hpp"

#include <jawt_md.h>
#include <vulkan/vulkan_xlib.h>

namespace minemod::jni {

VkResult create_drawing_surface(
    VkInstance instance, const JAWT_DrawingSurfaceInfo& info, VkSurfaceKHR& surface) noexcept {
    const auto* window = static_cast<const JAWT_X11DrawingSurfaceInfo*>(info.platformInfo);
    const VkXlibSurfaceCreateInfoKHR create_info{
        .sType = VK_STRUCTURE_TYPE_XLIB_SURFACE_CREATE_INFO_KHR,
        .pNext = nullptr,
        .flags = 0,
        .dpy = window->display,
        .window = window->drawable,
    };
    return vkCreateXlibSurfaceKHR(instance, &create_info, nullptr, &surface);
}

}
