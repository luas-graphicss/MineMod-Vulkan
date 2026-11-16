#pragma once

#include <cstdint>
#include <span>
#include <string>
#include <string_view>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

struct DeviceDescription {
    std::string name;
    std::string driver;
    VkPhysicalDeviceType type = VK_PHYSICAL_DEVICE_TYPE_OTHER;
    std::uint32_t api_version = 0;
};

struct SelectedDevice {
    VkPhysicalDevice handle = VK_NULL_HANDLE;
    std::uint32_t queue_family = 0;
    DeviceDescription description;
};

struct DeviceRequirements {
    std::span<const char* const> extensions;
    VkSurfaceKHR presentation_surface = VK_NULL_HANDLE;
};

SelectedDevice select_device(VkInstance instance, const DeviceRequirements& requirements);

bool supports_extension(VkPhysicalDevice device, std::string_view name);

std::string device_type_name(VkPhysicalDeviceType type);

std::string version_text(std::uint32_t api_version);

}
