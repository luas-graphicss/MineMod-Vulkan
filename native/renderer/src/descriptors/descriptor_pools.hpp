#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"

namespace minemod::renderer {

struct DescriptorPoolShape {
    std::uint32_t sets_per_pool;
    std::vector<VkDescriptorPoolSize> descriptors_per_pool;
    std::size_t max_pools;
};

class DescriptorPools {
public:
    DescriptorPools(const Device& device, DescriptorPoolShape shape);

    VkDescriptorSet allocate(VkDescriptorSetLayout layout);
    void reset();

    std::size_t pool_count() const noexcept {
        return pools_.size();
    }

private:
    std::optional<VkDescriptorSet> allocate_from_active_pool(VkDescriptorSetLayout layout);
    void activate_next_pool();
    void add_pool();

    VkDevice device_;
    DescriptorPoolShape shape_;
    std::vector<DeviceHandle<VkDescriptorPool>> pools_;
    std::size_t active_pool_ = 0;
    std::uint32_t sets_in_active_pool_ = 0;
};

}
