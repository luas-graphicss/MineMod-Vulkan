#pragma once

#include <array>
#include <cstdint>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "handoff/ownership_transfer.hpp"
#include "memory/shared_image.hpp"
#include "sharing/provider_sharing.hpp"

namespace minemod::scene {

struct AttachmentRole {
    VkFormat format;
    VkImageUsageFlags usage;
    VkImageAspectFlags aspect;
    VkImageLayout handoff_layout;
};

class InteropSharedTargets final : public SharedTargets {
public:
    InteropSharedTargets(const renderer::Device& device, VkExtent2D extent);

    const AttachmentImage& attachment(SharedAttachment which) const noexcept override;
    std::int64_t export_memory(SharedAttachment which) const override;
    void record_release(VkCommandBuffer commands) const override;
    void record_reclaim(VkCommandBuffer commands) const override;

private:
    struct Attachment {
        Attachment(const renderer::Device& device, VkExtent2D extent, const AttachmentRole& role);

        interop::SharedImage image;
        renderer::DeviceHandle<VkImageView> view;
        AttachmentImage described;
    };

    const Attachment& owned(SharedAttachment which) const noexcept;
    interop::HandoffResources handed_over() const;

    std::array<Attachment, shared_attachment_count> attachments_;
    interop::OwnershipTransfer release_;
    interop::OwnershipTransfer reclaim_;
};

}
