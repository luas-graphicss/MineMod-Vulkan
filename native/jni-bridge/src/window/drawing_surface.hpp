#pragma once

#include <jawt.h>
#include <vulkan/vulkan_core.h>

namespace minemod::jni {

VkResult create_drawing_surface(
    VkInstance instance, const JAWT_DrawingSurfaceInfo& info, VkSurfaceKHR& surface) noexcept;

}
