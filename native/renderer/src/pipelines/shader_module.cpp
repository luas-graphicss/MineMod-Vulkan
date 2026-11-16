#include "pipelines/shader_module.hpp"

namespace minemod::renderer {

DeviceHandle<VkShaderModule> create_shader_module(const Device& device, std::span<const std::uint32_t> code) {
    const VkShaderModuleCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO,
        .codeSize = code.size_bytes(),
        .pCode = code.data(),
    };
    return create_device_handle<VkShaderModule>(
        device.get(), create_info, vkCreateShaderModule, vkDestroyShaderModule, "creating a shader module");
}

}
