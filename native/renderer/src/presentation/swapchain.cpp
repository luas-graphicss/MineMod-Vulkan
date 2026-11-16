#include "presentation/swapchain.hpp"

#include <algorithm>
#include <array>
#include <cstdint>
#include <string>
#include <utility>

#include "device/vulkan_error.hpp"
#include "presentation/binary_semaphore.hpp"

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

std::vector<VkPresentModeKHR> list_present_modes(VkPhysicalDevice device, VkSurfaceKHR surface) {
    std::uint32_t count = 0;
    require_success(
        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, &count, nullptr), "counting the present modes");
    std::vector<VkPresentModeKHR> modes(count);
    require_success(
        vkGetPhysicalDeviceSurfacePresentModesKHR(device, surface, &count, modes.data()), "listing the present modes");
    return modes;
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

bool has_area(VkExtent2D extent) noexcept {
    return extent.width != 0 && extent.height != 0;
}

}

std::unique_ptr<Swapchain> Swapchain::create(
    const Device& device,
    VkSurfaceKHR surface,
    VkExtent2D desired_extent,
    PresentPacing pacing,
    std::unique_ptr<Swapchain> replaced) {
    VkSurfaceCapabilitiesKHR capabilities{};
    require_success(
        vkGetPhysicalDeviceSurfaceCapabilitiesKHR(device.physical(), surface, &capabilities),
        "querying the surface capabilities");
    const VkExtent2D extent = choose_extent(capabilities, desired_extent);
    if (!has_area(extent)) {
        return nullptr;
    }
    const VkSurfaceFormatKHR surface_format = choose_surface_format(device.physical(), surface);
    require_blit_target(device.physical(), surface_format.format, capabilities);

    const VkSwapchainCreateInfoKHR create_info{
        .sType = VK_STRUCTURE_TYPE_SWAPCHAIN_CREATE_INFO_KHR,
        .surface = surface,
        .minImageCount = choose_image_count(capabilities),
        .imageFormat = surface_format.format,
        .imageColorSpace = surface_format.colorSpace,
        .imageExtent = extent,
        .imageArrayLayers = 1,
        .imageUsage = VK_IMAGE_USAGE_TRANSFER_DST_BIT,
        .imageSharingMode = VK_SHARING_MODE_EXCLUSIVE,
        .preTransform = capabilities.currentTransform,
        .compositeAlpha = choose_composite_alpha(capabilities),
        .presentMode = choose_present_mode(list_present_modes(device.physical(), surface), pacing),
        .clipped = VK_TRUE,
        .oldSwapchain = replaced ? replaced->swapchain_.get() : VK_NULL_HANDLE,
    };
    return std::unique_ptr<Swapchain>(new Swapchain(device.get(), create_info));
}

Swapchain::Swapchain(VkDevice device, const VkSwapchainCreateInfoKHR& create_info)
    : device_(device), format_(create_info.imageFormat), extent_(create_info.imageExtent) {
    swapchain_ = create_device_handle<VkSwapchainKHR>(
        device, create_info, vkCreateSwapchainKHR, vkDestroySwapchainKHR, "creating the swapchain");

    std::uint32_t image_count = 0;
    require_success(
        vkGetSwapchainImagesKHR(device, swapchain_.get(), &image_count, nullptr), "counting the swapchain images");
    images_.resize(image_count);
    require_success(
        vkGetSwapchainImagesKHR(device, swapchain_.get(), &image_count, images_.data()),
        "listing the swapchain images");

    copied_.reserve(image_count);
    for (std::uint32_t image = 0; image < image_count; ++image) {
        copied_.push_back(create_binary_semaphore(device));
    }
}

Swapchain::~Swapchain() {
    vkDeviceWaitIdle(device_);
}

VkResult Swapchain::acquire(VkSemaphore signaled_on_acquisition, std::uint32_t& image_index) const {
    return vkAcquireNextImageKHR(
        device_, swapchain_.get(), UINT64_MAX, signaled_on_acquisition, VK_NULL_HANDLE, &image_index);
}

VkResult Swapchain::present(VkQueue queue, std::uint32_t image_index) const {
    const VkSwapchainKHR swapchain = swapchain_.get();
    const VkSemaphore copied = copied_semaphore(image_index);
    const VkPresentInfoKHR presentation{
        .sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR,
        .waitSemaphoreCount = 1,
        .pWaitSemaphores = &copied,
        .swapchainCount = 1,
        .pSwapchains = &swapchain,
        .pImageIndices = &image_index,
    };
    return vkQueuePresentKHR(queue, &presentation);
}

}
