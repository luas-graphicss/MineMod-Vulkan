#pragma once

#include <cstdint>
#include <memory>
#include <optional>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "minemod/abi/result.h"
#include "minemod/scene/scene_session.h"
#include "sharing/provider_sharing.hpp"

namespace minemod::scene {

enum class HandoffSynchronization { blocking, semaphores };

class SessionSharing {
public:
    explicit SessionSharing(const renderer::Device& device);

    std::unique_ptr<SharedTargets> create_targets(VkExtent2D extent);
    minemod_scene_sharing describe(const SharedTargets& targets, VkExtent2D extent) const;
    ExportedSemaphores export_semaphores();

    bool is_with_provider() const noexcept {
        return handoff_.has_value();
    }

    minemod_result check_release(HandoffSynchronization synchronization) const noexcept;
    renderer::SubmitSemaphores release_to_provider(HandoffSynchronization synchronization) noexcept;
    std::optional<renderer::SubmitSemaphores> reclaim_for_renderer() noexcept;

private:
    std::unique_ptr<ProviderSharing> provider_;
    std::optional<HandoffSynchronization> handoff_;
    std::uint32_t generation_ = 0;
    bool has_exported_semaphores_ = false;
};

}
