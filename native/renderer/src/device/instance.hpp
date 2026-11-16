#pragma once

#include <mutex>
#include <span>
#include <string>
#include <vector>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

enum class Validation { disabled, required };

class Instance {
public:
    Instance(Validation validation, std::span<const char* const> extensions);
    ~Instance();

    Instance(const Instance&) = delete;
    Instance& operator=(const Instance&) = delete;

    VkInstance get() const noexcept {
        return instance_;
    }

    std::vector<std::string> validation_messages() const;

private:
    static VKAPI_ATTR VkBool32 VKAPI_CALL record_message(
        VkDebugUtilsMessageSeverityFlagBitsEXT,
        VkDebugUtilsMessageTypeFlagsEXT,
        const VkDebugUtilsMessengerCallbackDataEXT* message,
        void* instance) noexcept;

    void create_messenger();

    VkInstance instance_ = VK_NULL_HANDLE;
    VkDebugUtilsMessengerEXT messenger_ = VK_NULL_HANDLE;
    mutable std::mutex messages_mutex_;
    std::vector<std::string> messages_;
};

}
