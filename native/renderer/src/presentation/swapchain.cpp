#include "presentation/swapchain.hpp"

#include <algorithm>
#include <array>
#include <cstdint>
#include <string>

#include "device/vulkan_error.hpp"

namespace minemod::renderer {

namespace {

constexpr std::uint32_t preferred_image_count = 2;
constexpr std::uint32_t undefined_extent = UINT32_MAX;

VkSurfaceFormatKHR choose_surface_format(VkPhysicalDevice device, VkSurfaceKHR surface) {
    std::uint32_t count = 0;
    require_success(
        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, &count, nullptr), "counting the surface formats");
    std::vector<VkSurfaceFormatKHR> formats(count);
    require_success(
        vkGetPhysicalDeviceSurfaceFormatsKHR(device, surface, &count, formats.data()), "listing the surface formats");
    if (formats.empty()) {
        throw UnsupportedSystem("the surface offers no presentation format");
    }
    const auto unorm = std::ranges::find_if(formats, [](const VkSurfaceFormatKHR& candidate) {
        return candidate.format == VK_FORMAT_B8G8R8A8_UNORM || candidate.format == VK_FORMAT_R8G8B8A8_UNORM;
    });
    return unorm == formats.end() ? formats.front() : *unorm;
}

void require_blit_target(VkPhysicalDevice device, VkFormat format, const VkSurfaceCapabilitiesKHR& capabilities) {
    VkFormatProperties properties{};
    vkGetPhysicalDeviceFormatProperties(device, format, &properties);
    const bool accepts_blits = (properties.optimalTilingFeatures & VK_FORMAT_FEATURE_BLIT_DST_BIT) != 0;
    const bool accepts_transfers = (capabilities.supportedUsageFlags & VK_IMAGE_USAGE_TRANSFER_DST_BIT) != 0;
    if (!accepts_blits || !accepts_transfers) {
        throw UnsupportedSystem(
            "the surface images of VkFormat " + std::to_string(format) + " cannot receive a blitted frame");
    }
}

VkExtent2D choose_extent(const VkSurfaceCapabilitiesKHR& capabilities, VkExtent2D desired) {
    if (capabilities.currentExtent.width != undefined_extent) {
        return capabilities.currentExtent;
    }
    return VkExtent2D{
        std::clamp(desired.width, capabilities.minImageExtent.width, capabilities.maxImageExtent.width),
        std::clamp(desired.height, capabilities.minImageExtent.height, capabilities.maxImageExtent.height),
    };
}

std::uint32_t choose_image_count(const VkSurfaceCapabilitiesKHR& capabilities) {
    const std::uint32_t wanted = std::max(preferred_image_count, capabilities.minImageCount);
    const bool is_bounded = capabilities.maxImageCount != 0;
    return is_bounded ? std::min(wanted, capabilities.maxImageCount) : wanted;
}

VkCompositeAlphaFlagBitsKHR choose_composite_alpha(const VkSurfaceCapabilitiesKHR& capabilities) {
    constexpr std::array preferences{
        VK_COMPOSITE_ALPHA_OPAQUE_BIT_KHR,
        VK_COMPOSITE_ALPHA_INHERIT_BIT_KHR,
        VK_COMPOSITE_ALPHA_PRE_MULTIPLIED_BIT_KHR,
        VK_COMPOSITE_ALPHA_POST_MULTIPLIED_BIT_KHR,
    };
    const auto supported = std::ranges::find_if(preferences, [&](VkCompositeAlphaFlagBitsKHR mode) {
        return (capabilities.supportedCompositeAlpha & mode) != 0;
    });
    if (supported == preferences.end()) {
        throw UnsupportedSystem("the surface supports no composite alpha mode");
    }
    return *supported;
}

}

Swapchain::Swapchain(const Device& device, VkSurfaceKHR surface, VkExtent2D desired_extent) : device_(device.get()) {
    VkSurfaceCapabilitiesKHR capabilities{};
    require_success(
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.physical(), surface, &capabilities),
        "querying the surface capabilities");
    const VkSurfaceFormatKHR surface_format = choose_surface_format(device.physical(), surface);
    require_blit_target(device.physical(), surface_format.format, capabilities);
    format_ = surface_format.format;
    extent_ = choose_extent(capabilities, desired_extent);

    const VkSwapchainCreateInfoKHR create_info{
        .sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR,
        .surface = surface,
        .minImageCount = choose_image_count(capabilities),
        .imageFormat = surface_format.format,
        .imageColorSpace = surface_format.colorSpace,
        .imageExtent = extent_,
        .imageArrayLayers = 1,
        .imageUsage = VK_IMAGE_USAGE_TRANSFER_DST_BIT,
        .imageSharingMode = VK_SHARING_MODE_EXCLUSIVE,
        .preTransform = capabilities.currentTransform,
        .compositeAlpha = choose_composite_alpha(capabilities),
        .presentMode = VK_PRESENT_MODE_FIFO_KHR,
        .clipped = VK_TRUE,
    };
    swapchain_ = create_device_handle<VkSwapchainKHR>(
        device.get(), create_info, vkCreateSwapchainKHR, vkDestroySwapchainKHR, "creating the swapchain");

    std::uint32_t image_count = 0;
    require_success(
        vkGetSwapchainImagesKHR(device.get(), swapchain_.get(), &image_count, nullptr), "counting the swapchain images");
    images_.resize(image_count);
    require_success(
        vkGetSwapchainImagesKHR(device.get(), swapchain_.get(), &image_count, images_.data()),
        "listing the swapchain images");
}

Swapchain::~Swapchain() {
    vkDeviceWaitIdle(device_);
}

VkResult Swapchain::acquire(VkSemaphore signaled_on_acquisition, std::uint32_t& image_index) const {
    return vkAcquireNextImageKHR(
        device_, swapchain_.get(), UINT64_MAX, signaled_on_acquisition, VK_NULL_HANDLE, &image_index);
}

VkResult Swapchain::present(VkQueue queue, VkSemaphore awaited, std::uint32_t image_index) const {
    const VkSwapchainKHR swapchain = swapchain_.get();
    const VkPresentInfoKHR presentation{
        .sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR,
        .waitSemaphoreCount = 1,
        .pWaitSemaphores = &awaited,
        .swapchainCount = 1,
        .pSwapchains = &swapchain,
        .pImageIndices = &image_index,
    };
    return vkQueuePresentKHR(queue, &presentation);
}

}
