#include "device/instance.hpp"

#include <algorithm>
#include <cstdint>
#include <string_view>

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

namespace {

constexpr const char* validation_layer = "VK_LAYER_KHRONOS_validation";

bool has_layer(std::string_view name) {
    std::uint32_t count = 0;
    require_success(vkEnumerateInstanceLayerProperties(&count, nullptr), "counting the instance layers");
    std::vector<VkLayerProperties> layers(count);
    require_success(vkEnumerateInstanceLayerProperties(&count, layers.data()), "listing the instance layers");
    return std::ranges::any_of(layers, [&](const VkLayerProperties& layer) { return name == layer.layerName; });
}

bool has_extension(std::string_view name) {
    std::uint32_t count = 0;
    require_success(
        vkEnumerateInstanceExtensionProperties(nullptr, &count, nullptr), "counting the instance extensions");
    std::vector<VkExtensionProperties> extensions(count);
    require_success(
        vkEnumerateInstanceExtensionProperties(nullptr, &count, extensions.data()), "listing the instance extensions");
    return std::ranges::any_of(
        extensions, [&](const VkExtensionProperties& extension) { return name == extension.extensionName; });
}

}

Instance::Instance(Validation validation, std::span<const char* const> extensions) {
    std::vector<const char*> enabled_extensions(extensions.begin(), extensions.end());
    std::vector<const char*> enabled_layers;
    if (validation == Validation::required) {
        if (!has_layer(validation_layer)) {
            throw UnsupportedSystem(
                std::string("validation was requested but ") + validation_layer + " is not installed");
        }
        enabled_layers.push_back(validation_layer);
        enabled_extensions.push_back(VK_EXT_DEBUG_UTILS_EXTENSION_NAME);
    }
    for (const char* extension : enabled_extensions) {
        if (!has_extension(extension)) {
            throw UnsupportedSystem(std::string("the Vulkan installation lacks the instance extension ") + extension);
        }
    }

    const VkApplicationInfo application{
        .sType = VK_STRUCTURE_TYPE_APPLICATION_INFO,
        .pApplicationName = "MineMod",
        .apiVersion = VK_API_VERSION_1_3,
    };
    const VkInstanceCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_INSTANCE_CREATE_INFO,
        .pApplicationInfo = &application,
        .enabledLayerCount = static_cast<std::uint32_t>(enabled_layers.size()),
        .ppEnabledLayerNames = enabled_layers.data(),
        .enabledExtensionCount = static_cast<std::uint32_t>(enabled_extensions.size()),
        .ppEnabledExtensionNames = enabled_extensions.data(),
    };
    require_success(vkCreateInstance(&create_info, nullptr, &instance_), "creating the Vulkan instance");

    if (validation == Validation::required) {
        try {
            create_messenger();
        } catch (...) {
            vkDestroyInstance(instance_, nullptr);
            throw;
        }
    }
}

Instance::~Instance() {
    if (messenger_ != VK_NULL_HANDLE) {
        const auto destroy = reinterpret_cast<PFN_vkDestroyDebugUtilsMessengerEXT>(
            vkGetInstanceProcAddr(instance_, "vkDestroyDebugUtilsMessengerEXT"));
        destroy(instance_, messenger_, nullptr);
    }
    vkDestroyInstance(instance_, nullptr);
}

std::vector<std::string> Instance::validation_messages() const {
    const std::scoped_lock lock(messages_mutex_);
    return messages_;
}

VkBool32 Instance::record_message(
    VkDebugUtilsMessageSeverityFlagBitsEXT,
    VkDebugUtilsMessageTypeFlagsEXT,
    const VkDebugUtilsMessengerCallbackDataEXT* message,
    void* instance) noexcept {
    auto& self = *static_cast<Instance*>(instance);
    const std::scoped_lock lock(self.messages_mutex_);
    self.messages_.emplace_back(message->pMessage);
    return VK_FALSE;
}

void Instance::create_messenger() {
    const auto create = reinterpret_cast<PFN_vkCreateDebugUtilsMessengerEXT>(
        vkGetInstanceProcAddr(instance_, "vkCreateDebugUtilsMessengerEXT"));
    if (create == nullptr) {
        throw UnsupportedSystem("the Vulkan loader does not expose vkCreateDebugUtilsMessengerEXT");
    }
    const VkDebugUtilsMessengerCreateInfoEXT create_info{
        .sType = VK_STRUCTURE_TYPE_DEBUG_UTILS_MESSENGER_CREATE_INFO_EXT,
        .messageSeverity =
            VK_DEBUG_UTILS_MESSAGE_SEVERITY_WARNING_BIT_EXT | VK_DEBUG_UTILS_MESSAGE_SEVERITY_ERROR_BIT_EXT,
        .messageType = VK_DEBUG_UTILS_MESSAGE_TYPE_VALIDATION_BIT_EXT,
        .pfnUserCallback = &Instance::record_message,
        .pUserData = this,
    };
    require_success(create(instance_, &create_info, nullptr, &messenger_), "creating the validation messenger");
}

}
