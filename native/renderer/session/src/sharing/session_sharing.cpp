#include "sharing/session_sharing.hpp"

#include <algorithm>
#include <cstddef>

namespace minemod::scene {

namespace {

static_assert(VK_UUID_SIZE == MINEMOD_SCENE_DEVICE_UUID_BYTES);

minemod_shared_attachment describe_attachment(const AttachmentImage& image, VkExtent2D extent) {
    return minemod_shared_attachment{
        .allocation_bytes = image.allocation_bytes,
        .vulkan_format = static_cast<std::uint32_t>(image.format),
        .vulkan_layout = static_cast<std::uint32_t>(image.handoff_layout),
        .width = extent.width,
        .height = extent.height,
    };
}

}

SessionSharing::SessionSharing(const renderer::Device& device) : provider_(open_provider_sharing(device)) {}

std::unique_ptr<SharedTargets> SessionSharing::create_targets(VkExtent2D extent) {
    std::unique_ptr<SharedTargets> targets = provider_->create_targets(extent);
    ++generation_;
    return targets;
}

minemod_scene_sharing SessionSharing::describe(const SharedTargets& targets, VkExtent2D extent) const {
    const DeviceUuids identity = provider_->device_identity();
    minemod_scene_sharing sharing{};
    std::ranges::copy(identity.device, sharing.device_uuid);
    std::ranges::copy(identity.driver, sharing.driver_uuid);
    for (const SharedAttachment which : {SharedAttachment::color, SharedAttachment::depth_stencil}) {
        sharing.attachments[static_cast<std::size_t>(which)] = describe_attachment(targets.attachment(which), extent);
    }
    sharing.flags = provider_->shares_semaphores() ? MINEMOD_SCENE_SHARING_SEMAPHORES : 0;
    sharing.generation = generation_;
    return sharing;
}

ExportedSemaphores SessionSharing::export_semaphores() const {
    return provider_->export_semaphores();
}

}
