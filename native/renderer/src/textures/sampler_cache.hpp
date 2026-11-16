#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"

namespace minemod::renderer {

struct SamplerDescription {
    VkFilter magnification;
    VkFilter minification;
    VkSamplerMipmapMode mip_mode;
    VkSamplerAddressMode address_mode;
    std::uint32_t mip_levels;

    friend bool operator==(const SamplerDescription&, const SamplerDescription&) = default;
};

class SamplerCache {
public:
    explicit SamplerCache(const Device& device) noexcept : device_(device.get()) {}

    VkSampler sampler_for(const SamplerDescription& description);

    std::size_t sampler_count() const noexcept {
        return samplers_.size();
    }

private:
    struct CachedSampler {
        SamplerDescription description;
        DeviceHandle<VkSampler> sampler;
    };

    VkDevice device_;
    std::vector<CachedSampler> samplers_;
};

}
