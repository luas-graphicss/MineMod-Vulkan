#include "presentation/swapchain.hpp"

#include <algorithm>
#include <array>
#include <cstdint>
#include <string>

#include "device/vulkan_error.hpp"
#include "resources/image_layout.hpp"

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

void blit_frame(VkCommandBuffer commands, const Image& source, VkImage target, VkExtent2D target_extent) {
    const VkExtent2D source_extent = source.extent();
    const VkImageBlit2 region{
        .sType = VK_STRUCTURE_TYPE_IMAGE_BLIT_2,
        .srcSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
        .srcOffsets = {
            VkOffset3D{0, 0, 0},
            VkOffset3D{static_cast<std::int32_t>(source_extent.width), static_cast<std::int32_t>(source_extent.height), 1},
        },
        .dstSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
        .dstOffsets = {
            VkOffset3D{0, 0, 0},
            VkOffset3D{static_cast<std::int32_t>(target_extent.width), static_cast<std::int32_t>(target_extent.height), 1},
        },
    };
    const VkBlitImageInfo2 blit{
        .sType = VK_STRUCTURE_TYPE_BLIT_IMAGE_INFO_2,
        .srcImage = source.get(),
        .srcImageLayout = VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
        .dstImage = target,
        .dstImageLayout = VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
        .regionCount = 1,
        .pRegions = &region,
        .filter = VK_FILTER_NEAREST,
    };
    vkCmdBlitImage2(commands, &blit);
}

DeviceHandle<VkSemaphore> create_semaphore(VkDevice device) {
    const VkSemaphoreCreateInfo create_info{.sType = VK_STRUCTURE_TYPE_SEMAPHORE_CREATE_INFO};
    return create_device_handle<VkSemaphore>(
        device, create_info, vkCreateSemaphore, vkDestroySemaphore, "creating a presentation semaphore");
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

    image_acquired_ = create_semaphore(device.get());
    frame_copied_ = create_semaphore(device.get());
}

Swapchain::~Swapchain() {
    // A presented frame keeps its semaphore in use until the queue drains.
    vkDeviceWaitIdle(device_);
}

void Swapchain::present(const Device& device, const Image& transfer_source) {
    std::uint32_t image_index = 0;
    const VkResult acquired = vkAcquireNextImageKHR(
        device.get(), swapchain_.get(), UINT64_MAX, image_acquired_.get(), VK_NULL_HANDLE, &image_index);
    if (acquired != VK_SUBOPTIMAL_KHR) {
        require_success(acquired, "acquiring a swapchain image");
    }

    const VkImage target = images_[image_index];
    device.submit(
        [&](VkCommandBuffer commands) {
            change_layout(commands, target, VK_IMAGE_ASPECT_COLOR_BIT, undefined_to_transfer_destination);
            blit_frame(commands, transfer_source, target, extent_);
            change_layout(commands, target, VK_IMAGE_ASPECT_COLOR_BIT, transfer_destination_to_present);
        },
        SubmitSemaphores{
            .wait = image_acquired_.get(),
            .wait_stage = VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
            .signal = frame_copied_.get(),
        });

    const VkSemaphore copied = frame_copied_.get();
    const VkSwapchainKHR swapchain = swapchain_.get();
    const VkPresentInfoKHR presentation{
        .sType = VK_STRUCTURE_TYPE_PRESENT_INFO_KHR,
        .waitSemaphoreCount = 1,
        .pWaitSemaphores = &copied,
        .swapchainCount = 1,
        .pSwapchains = &swapchain,
        .pImageIndices = &image_index,
    };
    const VkResult presented = vkQueuePresentKHR(device.queue(), &presentation);
    if (presented != VK_SUBOPTIMAL_KHR) {
        require_success(presented, "presenting a swapchain image");
    }
}

}
