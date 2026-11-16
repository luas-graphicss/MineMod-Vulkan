#include "descriptors/descriptor_layout.hpp"

#include <vector>

namespace minemod::renderer {

DeviceHandle<VkDescriptorSetLayout> create_descriptor_layout(
    const Device& device, std::span<const DescriptorBinding> bindings) {
    std::vector<VkDescriptorSetLayoutBinding> layout_bindings;
    layout_bindings.reserve(bindings.size());
    for (const DescriptorBinding& binding : bindings) {
        layout_bindings.push_back(VkDescriptorSetLayoutBinding{
            .binding = binding.binding,
            .descriptorType = binding.type,
            .descriptorCount = 1,
            .stageFlags = binding.stages,
        });
    }
    const VkDescriptorSetLayoutCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO,
        .bindingCount = static_cast<std::uint32_t>(layout_bindings.size()),
        .pBindings = layout_bindings.data(),
    };
    return create_device_handle<VkDescriptorSetLayout>(
        device.get(),
        create_info,
        vkCreateDescriptorSetLayout,
        vkDestroyDescriptorSetLayout,
        "creating a descriptor set layout");
}

}
