#include "handoff/shared_resource_handoff.hpp"

#include <stdexcept>

namespace minemod::interop {
namespace {

HandoffSemaphores require_one_per_direction(const HandoffSemaphores& semaphores) {
    const bool missing = semaphores.renderer_done == VK_NULL_HANDLE || semaphores.provider_done == VK_NULL_HANDLE;
    if (missing || semaphores.renderer_done == semaphores.provider_done) {
        throw std::invalid_argument("a handoff needs one semaphore for each direction");
    }
    return semaphores;
}

}

SharedResourceHandoff::SharedResourceHandoff(
    const renderer::Device& device, const HandoffResources& resources, const HandoffSemaphores& semaphores)
    : device_(device),
      semaphores_(require_one_per_direction(semaphores)),
      release_(OwnershipTransfer::release_from(device.queue_family(), resources)),
      acquire_(OwnershipTransfer::acquire_for(device.queue_family(), resources)) {}

void SharedResourceHandoff::release_to_provider() {
    if (owner_ == ResourceOwner::provider) {
        throw std::logic_error("the shared resources were already released to the OpenGL provider");
    }
    device_.submit(
        [this](VkCommandBuffer commands) { release_.record(commands); }, {.signal = semaphores_.renderer_done});
    owner_ = ResourceOwner::provider;
}

void SharedResourceHandoff::reclaim_for_renderer() {
    if (owner_ == ResourceOwner::renderer) {
        throw std::logic_error("the renderer already owns the shared resources");
    }
    device_.submit(
        [this](VkCommandBuffer commands) { acquire_.record(commands); },
        {.wait = semaphores_.provider_done, .wait_stage = VK_PIPELINE_STAGE_2_ALL_COMMANDS_BIT});
    owner_ = ResourceOwner::renderer;
    ++completed_round_trips_;
}

}
