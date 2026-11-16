#include "device/physical_device.hpp"

#include <algorithm>
#include <optional>
#include <string_view>
#include <utility>
#include <vector>

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

namespace {

int preference(VkPhysicalDeviceType type) {
    switch (type) {
        case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU:
            return 4;
        case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU:
            return 3;
        case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU:
            return 2;
        case VK_PHYSICAL_DEVICE_TYPE_CPU:
            return 1;
        default:
            return 0;
    }
}

DeviceDescription describe(VkPhysicalDevice device) {
    VkPhysicalDeviceDriverProperties driver{.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_DRIVER_PROPERTIES};
    VkPhysicalDeviceProperties2 properties{.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2, .pNext = &driver};
    vkGetPhysicalDeviceProperties2(device, &properties);
    return DeviceDescription{
        .name = properties.properties.deviceName,
        .driver = std::string(driver.driverName) + " " + driver.driverInfo,
        .type = properties.properties.deviceType,
        .api_version = properties.properties.apiVersion,
    };
}

bool has_extension(VkPhysicalDevice device, std::string_view name) {
    std::uint32_t count = 0;
    require_success(
        vkEnumerateDeviceExtensionProperties(device, nullptr, &count, nullptr), "counting the device extensions");
    std::vector<VkExtensionProperties> extensions(count);
    require_success(
        vkEnumerateDeviceExtensionProperties(device, nullptr, &count, extensions.data()),
        "listing the device extensions");
    return std::ranges::any_of(
        extensions, [&](const VkExtensionProperties& extension) { return name == extension.extensionName; });
}

std::optional<std::uint32_t> find_queue_family(VkPhysicalDevice device, VkSurfaceKHR presentation_surface) {
    std::uint32_t count = 0;
    vkGetPhysicalDeviceQueueFamilyProperties(device, &count, nullptr);
    std::vector<VkQueueFamilyProperties> families(count);
    vkGetPhysicalDeviceQueueFamilyProperties(device, &count, families.data());

    for (std::uint32_t family = 0; family < count; ++family) {
        if ((families[family].queueFlags & VK_QUEUE_GRAPHICS_BIT) == 0) {
            continue;
        }
        VkBool32 presents = VK_TRUE;
        if (presentation_surface != VK_NULL_HANDLE) {
            require_success(
                vkGetPhysicalDeviceSurfaceSupportKHR(device, family, presentation_surface, &presents),
                "querying presentation support");
        }
        if (presents == VK_TRUE) {
            return family;
        }
    }
    return std::nullopt;
}

std::optional<std::string> missing_requirement(
    VkPhysicalDevice device, const DeviceDescription& description, const DeviceRequirements& requirements) {
    if (description.api_version < VK_API_VERSION_1_3) {
        return "supports only Vulkan " + version_text(description.api_version);
    }

    VkPhysicalDeviceVulkan13Features core_features{.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_VULKAN_1_3_FEATURES};
    VkPhysicalDeviceFeatures2 features{.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_FEATURES_2, .pNext = &core_features};
    vkGetPhysicalDeviceFeatures2(device, &features);
    if (core_features.dynamicRendering != VK_TRUE) {
        return "lacks the dynamicRendering feature";
    }
    if (core_features.synchronization2 != VK_TRUE) {
        return "lacks the synchronization2 feature";
    }

    for (const char* extension : requirements.extensions) {
        if (!has_extension(device, extension)) {
            return std::string("lacks the device extension ") + extension;
        }
    }
    return std::nullopt;
}

std::string joined(const std::vector<std::string>& parts) {
    std::string text;
    for (const std::string& part : parts) {
        text += text.empty() ? ": " : "; ";
        text += part;
    }
    return text;
}

}

SelectedDevice select_device(VkInstance instance, const DeviceRequirements& requirements) {
    std::uint32_t count = 0;
    require_success(vkEnumeratePhysicalDevices(instance, &count, nullptr), "counting the Vulkan devices");
    std::vector<VkPhysicalDevice> devices(count);
    require_success(vkEnumeratePhysicalDevices(instance, &count, devices.data()), "listing the Vulkan devices");
    if (devices.empty()) {
        throw UnsupportedSystem("no Vulkan device is installed");
    }

    std::optional<SelectedDevice> selected;
    std::vector<std::string> rejections;
    for (const VkPhysicalDevice device : devices) {
        DeviceDescription description = describe(device);
        if (const auto missing = missing_requirement(device, description, requirements)) {
            rejections.push_back(description.name + " " + *missing);
            continue;
        }
        const auto queue_family = find_queue_family(device, requirements.presentation_surface);
        if (!queue_family) {
            rejections.push_back(description.name + " has no graphics queue for the requested presentation");
            continue;
        }
        if (!selected || preference(description.type) > preference(selected->description.type)) {
            selected = SelectedDevice{device, *queue_family, std::move(description)};
        }
    }
    if (!selected) {
        throw UnsupportedSystem("no Vulkan device qualifies" + joined(rejections));
    }
    return *selected;
}

std::string device_type_name(VkPhysicalDeviceType type) {
    switch (type) {
        case VK_PHYSICAL_DEVICE_TYPE_DISCRETE_GPU:
            return "discrete GPU";
        case VK_PHYSICAL_DEVICE_TYPE_INTEGRATED_GPU:
            return "integrated GPU";
        case VK_PHYSICAL_DEVICE_TYPE_VIRTUAL_GPU:
            return "virtual GPU";
        case VK_PHYSICAL_DEVICE_TYPE_CPU:
            return "CPU";
        default:
            return "other device";
    }
}

std::string version_text(std::uint32_t api_version) {
    return std::to_string(VK_API_VERSION_MAJOR(api_version)) + "." + std::to_string(VK_API_VERSION_MINOR(api_version))
        + "." + std::to_string(VK_API_VERSION_PATCH(api_version));
}

}
