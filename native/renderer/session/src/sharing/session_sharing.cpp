#include "sharing/session_sharing.hpp"

#include <algorithm>
#include <cstddef>
#include <utility>

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
    sharing.flags = provider_->semaphores() ? MINEMOD_SCENE_SHARING_SEMAPHORES : 0;
    sharing.generation = generation_;
    return sharing;
}

ExportedSemaphores SessionSharing::export_semaphores() {
    const ExportedSemaphores exported = provider_->export_semaphores();
    has_exported_semaphores_ = true;
    return exported;
}

minemod_result SessionSharing::check_release(HandoffSynchronization synchronization) const noexcept {
    if (synchronization == HandoffSynchronization::blocking) {
        return MINEMOD_RESULT_OK;
    }
    if (!provider_->semaphores()) {
        return MINEMOD_RESULT_UNSUPPORTED_CAPABILITY;
    }
    return has_exported_semaphores_ ? MINEMOD_RESULT_OK : MINEMOD_RESULT_INVALID_ARGUMENT;
}

renderer::SubmitSemaphores SessionSharing::release_to_provider(HandoffSynchronization synchronization) noexcept {
    handoff_ = synchronization;
    if (synchronization == HandoffSynchronization::blocking) {
        return renderer::SubmitSemaphores{};
    }
    return renderer::SubmitSemaphores{.signal = provider_->semaphores()->renderer_done};
}

std::optional<renderer::SubmitSemaphores> SessionSharing::reclaim_for_renderer() noexcept {
    const HandoffSynchronization synchronization = *std::exchange(handoff_, std::nullopt);
    if (synchronization == HandoffSynchronization::blocking) {
        return std::nullopt;
    }
    return renderer::SubmitSemaphores{
        .wait = provider_->semaphores()->provider_done,
        .wait_stage = VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT,
    };
}

}
