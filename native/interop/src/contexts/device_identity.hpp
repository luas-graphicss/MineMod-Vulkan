#pragma once

#include <array>
#include <cstdint>

#include <vulkan/vulkan.h>

namespace minemod::interop {

struct DeviceIdentity {
    std::array<std::uint8_t, VK_UUID_SIZE> device_uuid;
    std::array<std::uint8_t, VK_UUID_SIZE> driver_uuid;

    friend bool operator==(const DeviceIdentity&, const DeviceIdentity&) = default;
};

DeviceIdentity identify_device(VkPhysicalDevice device);
void require_same_gpu(const DeviceIdentity& renderer_device, const DeviceIdentity& provider_device);

}
