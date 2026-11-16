#pragma once

#include <array>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

inline constexpr std::array<const char*, 2> headless_surface_extensions{
    VK_KHR_SURFACE_EXTENSION_NAME,
    VK_EXT_HEADLESS_SURFACE_EXTENSION_NAME,
};

class HeadlessSurface {
public:
    explicit HeadlessSurface(VkInstance instance);
    ~HeadlessSurface();

    HeadlessSurface(const HeadlessSurface&) = delete;
    HeadlessSurface& operator=(const HeadlessSurface&) = delete;

    VkSurfaceKHR get() const noexcept {
        return surface_;
    }

private:
    VkInstance instance_;
    VkSurfaceKHR surface_ = VK_NULL_HANDLE;
};

}
