#pragma once

#include <stdexcept>
#include <string>
#include <string_view>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

class VulkanError : public std::runtime_error {
public:
    VulkanError(std::string_view operation, VkResult result);

    VkResult result() const noexcept {
        return result_;
    }

private:
    VkResult result_;
};

class UnsupportedSystem : public std::runtime_error {
public:
    using std::runtime_error::runtime_error;
};

std::string result_name(VkResult result);

void require_success(VkResult result, std::string_view operation);

}
