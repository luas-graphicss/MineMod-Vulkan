#include "textures/sampler_cache.hpp"

#include <algorithm>
#include <stdexcept>

namespace minemod::renderer {

VkSampler SamplerCache::sampler_for(const SamplerDescription& description) {
    if (description.mip_levels == 0) {
        throw std::invalid_argument("a sampler covers at least one mip level");
    }
    const auto cached = std::ranges::find(samplers_, description, &CachedSampler::description);
    if (cached != samplers_.end()) {
        return cached->sampler.get();
    }

    const VkSamplerCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO,
        .magFilter = description.magnification,
        .minFilter = description.minification,
        .mipmapMode = description.mip_mode,
        .addressModeU = description.address_mode,
        .addressModeV = description.address_mode,
        .addressModeW = description.address_mode,
        .maxLod = static_cast<float>(description.mip_levels - 1),
    };
    samplers_.push_back(CachedSampler{
        description,
        create_device_handle<VkSampler>(device_, create_info, vkCreateSampler, vkDestroySampler, "creating a sampler"),
    });
    return samplers_.back().sampler.get();
}

}
