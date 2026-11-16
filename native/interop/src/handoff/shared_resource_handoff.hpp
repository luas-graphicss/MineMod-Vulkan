#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "handoff/ownership_transfer.hpp"

namespace minemod::interop {

struct HandoffSemaphores {
    VkSemaphore renderer_done;
    VkSemaphore provider_done;
};

enum class ResourceOwner { renderer, provider };

class SharedResourceHandoff {
public:
    SharedResourceHandoff(
        const renderer::Device& device, const HandoffResources& resources, const HandoffSemaphores& semaphores);

    void release_to_provider();
    void reclaim_for_renderer();

    ResourceOwner owner() const noexcept {
        return owner_;
    }

    std::uint64_t completed_round_trips() const noexcept {
        return completed_round_trips_;
    }

private:
    const renderer::Device& device_;
    HandoffSemaphores semaphores_;
    OwnershipTransfer release_;
    OwnershipTransfer acquire_;
    ResourceOwner owner_ = ResourceOwner::renderer;
    std::uint64_t completed_round_trips_ = 0;
};

}
