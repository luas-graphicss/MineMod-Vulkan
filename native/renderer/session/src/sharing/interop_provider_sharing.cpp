#include "sharing/provider_sharing.hpp"

#include <optional>

#include "contexts/device_identity.hpp"
#include "device/physical_device.hpp"
#include "device/vulkan_error.hpp"
#include "memory/owned_descriptor.hpp"
#include "memory/shared_image.hpp"
#include "sharing/interop_shared_targets.hpp"
#include "sync/shared_semaphore.hpp"

namespace minemod::scene {

namespace {

bool shares_semaphores_with_providers(const renderer::Device& device) {
    return renderer::supports_extension(device.physical(), interop::shared_semaphore_extensions.front())
        && interop::can_export_semaphores(device.physical());
}

class InteropProviderSharing final : public ProviderSharing {
public:
    explicit InteropProviderSharing(const renderer::Device& device) : device_(device) {
        if (shares_semaphores_with_providers(device)) {
            signals_.emplace(interop::SharedSemaphore(device), interop::SharedSemaphore(device));
        }
    }

    DeviceUuids device_identity() const override {
        const interop::DeviceIdentity identity = interop::identify_device(device_.physical());
        return DeviceUuids{identity.device_uuid, identity.driver_uuid};
    }

    std::unique_ptr<SharedTargets> create_targets(VkExtent2D extent) const override {
        return std::make_unique<InteropSharedTargets>(device_, extent);
    }

    std::optional<HandoffSemaphores> semaphores() const noexcept override {
        if (!signals_) {
            return std::nullopt;
        }
        return HandoffSemaphores{signals_->renderer_done.get(), signals_->provider_done.get()};
    }

    ExportedSemaphores export_semaphores() const override {
        if (!signals_) {
            throw renderer::UnsupportedSystem("the device shares no semaphores with an OpenGL provider");
        }
        interop::OwnedDescriptor renderer_done = signals_->renderer_done.export_descriptor();
        interop::OwnedDescriptor provider_done = signals_->provider_done.export_descriptor();
        return ExportedSemaphores{renderer_done.release(), provider_done.release()};
    }

private:
    struct Signals {
        interop::SharedSemaphore renderer_done;
        interop::SharedSemaphore provider_done;
    };

    const renderer::Device& device_;
    std::optional<Signals> signals_;
};

}

SharingExtensions sharing_extensions() {
    return SharingExtensions{interop::shared_image_extensions, interop::shared_semaphore_extensions};
}

std::unique_ptr<ProviderSharing> open_provider_sharing(const renderer::Device& device) {
    return std::make_unique<InteropProviderSharing>(device);
}

}
