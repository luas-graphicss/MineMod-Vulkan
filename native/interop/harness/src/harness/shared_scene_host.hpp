#pragma once

#include <cstdint>
#include <map>
#include <memory>
#include <string>

#include "contexts/device_identity.hpp"
#include "device/device.hpp"
#include "device/instance.hpp"
#include "harness/scene_image.hpp"
#include "memory/external_support.hpp"

namespace minemod::interop::harness {

class SharedSceneHost {
public:
    using ImageId = std::uint32_t;

    SharedSceneHost();

    const renderer::Device& device() const noexcept {
        return device_;
    }

    ImageId create_image(const SceneImageDescription& description);
    void destroy_image(ImageId image);
    const SceneImage& image(ImageId image) const;

    DeviceIdentity identity() const;
    ExternalSupport support_for(SceneImageKind kind, VkFormat format) const;
    bool can_share_semaphores() const;
    std::string validation_text() const;

private:
    renderer::Instance instance_;
    renderer::Device device_;
    std::map<ImageId, std::unique_ptr<SceneImage>> images_;
    ImageId next_image_ = 1;
};

}
