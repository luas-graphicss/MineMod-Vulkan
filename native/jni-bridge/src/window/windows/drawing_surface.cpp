#include "window/drawing_surface.hpp"

#include <windows.h>

#include <jawt_md.h>
#include <vulkan/vulkan_win32.h>

namespace minemod::jni {

VkResult create_drawing_surface(
    VkInstance instance, const JAWT_DrawingSurfaceInfo& info, VkSurfaceKHR& surface) noexcept {
    const auto* window = static_cast<const JAWT_Win32DrawingSurfaceInfo*>(info.platformInfo);
    const VkWin32SurfaceCreateInfoKHR create_info{
        .sType = VK_STRUCTURE_TYPE_WIN32_SURFACE_CREATE_INFO_KHR,
        .pNext = nullptr,
        .flags = 0,
        .hinstance = GetModuleHandleW(nullptr),
        .hwnd = window->hwnd,
    };
    return vkCreateWin32SurfaceKHR(instance, &create_info, nullptr, &surface);
}

}
