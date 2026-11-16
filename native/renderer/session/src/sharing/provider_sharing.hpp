#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <memory>
#include <span>

#include <vulkan/vulkan.h>

#include "device/device.hpp"

namespace minemod::scene {

enum class SharedAttachment : std::uint32_t { color, depth_stencil };

inline constexpr std::size_t shared_attachment_count = 2;

struct AttachmentImage {
    VkImage image;
    VkImageView view;
    VkFormat format;
    VkImageAspectFlags aspect;
    VkImageLayout handoff_layout;
    VkDeviceSize allocation_bytes;
};

class SharedTargets {
public:
    virtual ~SharedTargets() = default;

    virtual const AttachmentImage& attachment(SharedAttachment which) const noexcept = 0;
    virtual std::int64_t export_memory(SharedAttachment which) const = 0;
};

struct ExportedSemaphores {
    std::int64_t renderer_done;
    std::int64_t provider_done;
};

struct DeviceUuids {
    std::array<std::uint8_t, VK_UUID_SIZE> device;
    std::array<std::uint8_t, VK_UUID_SIZE> driver;
};

class ProviderSharing {
public:
    virtual ~ProviderSharing() = default;

    virtual DeviceUuids device_identity() const = 0;
    virtual std::unique_ptr<SharedTargets> create_targets(VkExtent2D extent) const = 0;
    virtual bool shares_semaphores() const noexcept = 0;
    virtual ExportedSemaphores export_semaphores() const = 0;
};

struct SharingExtensions {
    std::span<const char* const> attachments;
    std::span<const char* const> semaphores;
};

SharingExtensions sharing_extensions();
std::unique_ptr<ProviderSharing> open_provider_sharing(const renderer::Device& device);

}
