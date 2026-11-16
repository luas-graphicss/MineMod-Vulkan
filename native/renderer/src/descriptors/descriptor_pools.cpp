#include "descriptors/descriptor_pools.hpp"

#include <stdexcept>
#include <utility>

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

DescriptorPools::DescriptorPools(const Device& device, DescriptorPoolShape shape)
    : device_(device.get()), shape_(std::move(shape)) {
    if (shape_.sets_per_pool == 0 || shape_.max_pools == 0) {
        throw std::invalid_argument("descriptor pools need room for at least one set in one pool");
    }
    add_pool();
}

VkDescriptorSet DescriptorPools::allocate(VkDescriptorSetLayout layout) {
    for (;;) {
        if (const std::optional<VkDescriptorSet> set = allocate_from_active_pool(layout)) {
            return *set;
        }
        activate_next_pool();
    }
}

void DescriptorPools::reset() {
    for (const DeviceHandle<VkDescriptorPool>& pool : pools_) {
        require_success(vkResetDescriptorPool(device_, pool.get(), 0), "resetting a descriptor pool");
    }
    active_pool_ = 0;
    sets_in_active_pool_ = 0;
}

std::optional<VkDescriptorSet> DescriptorPools::allocate_from_active_pool(VkDescriptorSetLayout layout) {
    if (sets_in_active_pool_ == shape_.sets_per_pool) {
        return std::nullopt;
    }
    const VkDescriptorSetAllocateInfo allocation{
        .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO,
        .descriptorPool = pools_[active_pool_].get(),
        .descriptorSetCount = 1,
        .pSetLayouts = &layout,
    };
    VkDescriptorSet set = VK_NULL_HANDLE;
    const VkResult result = vkAllocateDescriptorSets(device_, &allocation, &set);
    if (result == VK_ERROR_OUT_OF_POOL_MEMORY || result == VK_ERROR_FRAGMENTED_POOL) {
        return std::nullopt;
    }
    require_success(result, "allocating a descriptor set");
    ++sets_in_active_pool_;
    return set;
}

void DescriptorPools::activate_next_pool() {
    if (active_pool_ + 1 == shape_.max_pools) {
        throw std::length_error("the descriptor pools reached their limit");
    }
    ++active_pool_;
    sets_in_active_pool_ = 0;
    if (active_pool_ == pools_.size()) {
        add_pool();
    }
}

void DescriptorPools::add_pool() {
    const VkDescriptorPoolCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,
        .maxSets = shape_.sets_per_pool,
        .poolSizeCount = static_cast<std::uint32_t>(shape_.descriptors_per_pool.size()),
        .pPoolSizes = shape_.descriptors_per_pool.data(),
    };
    pools_.push_back(create_device_handle<VkDescriptorPool>(
        device_, create_info, vkCreateDescriptorPool, vkDestroyDescriptorPool, "creating a descriptor pool"));
}

}
