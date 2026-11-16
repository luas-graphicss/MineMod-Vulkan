#pragma once

#include <cstdint>
#include <memory>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "minemod/scene/scene_session.h"
#include "sharing/provider_sharing.hpp"

namespace minemod::scene {

class SessionSharing {
public:
    explicit SessionSharing(const renderer::Device& device);

    std::unique_ptr<SharedTargets> create_targets(VkExtent2D extent);
    minemod_scene_sharing describe(const SharedTargets& targets, VkExtent2D extent) const;
    ExportedSemaphores export_semaphores() const;

private:
    std::unique_ptr<ProviderSharing> provider_;
    std::uint32_t generation_ = 0;
};

}
