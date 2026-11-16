#include "contexts/device_identity.hpp"

#include <algorithm>

#include "device/vulkan_error.hpp"

namespace minemod::interop {

DeviceIdentity identify_device(VkPhysicalDevice device) {
    VkPhysicalDeviceIDProperties identifiers{.sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_ID_PROPERTIES};
    VkPhysicalDeviceProperties2 properties{
        .sType = VK_STRUCTURE_TYPE_PHYSICAL_DEVICE_PROPERTIES_2,
        .pNext = &identifiers,
    };
    vkGetPhysicalDeviceProperties2(device, &properties);

    DeviceIdentity identity{};
    std::copy_n(identifiers.deviceUUID, VK_UUID_SIZE, identity.device_uuid.begin());
    std::copy_n(identifiers.driverUUID, VK_UUID_SIZE, identity.driver_uuid.begin());
    return identity;
}

void require_same_gpu(const DeviceIdentity& renderer_device, const DeviceIdentity& provider_device) {
    if (renderer_device != provider_device) {
        throw renderer::UnsupportedSystem(
            "the OpenGL provider runs on a different GPU or driver than the renderer, so they cannot share images");
    }
}

}
