#include "harness/shared_scene_host.hpp"

#include <stdexcept>

#include "device/physical_device.hpp"
#include "sync/shared_semaphore.hpp"

namespace minemod::interop::harness {

SharedSceneHost::SharedSceneHost()
    : instance_(renderer::Validation::required, {}),
      device_(
          renderer::select_device(instance_.get(), renderer::DeviceRequirements{shared_image_extensions}),
          shared_image_extensions) {}

SharedSceneHost::ImageId SharedSceneHost::create_image(const SceneImageDescription& description) {
    const ImageId image = next_image_++;
    images_.emplace(image, std::make_unique<SceneImage>(device_, description));
    return image;
}

void SharedSceneHost::destroy_image(ImageId image) {
    if (images_.erase(image) == 0) {
        throw std::invalid_argument("scene image " + std::to_string(image) + " does not exist in this host");
    }
}

const SceneImage& SharedSceneHost::image(ImageId image) const {
    const auto found = images_.find(image);
    if (found == images_.end()) {
        throw std::invalid_argument("scene image " + std::to_string(image) + " does not exist in this host");
    }
    return *found->second;
}

DeviceIdentity SharedSceneHost::identity() const {
    return identify_device(device_.physical());
}

ExternalSupport SharedSceneHost::support_for(SceneImageKind kind, VkFormat format) const {
    return query_external_support(device_.physical(), format, scene_image_usage(kind));
}

bool SharedSceneHost::can_share_semaphores() const {
    return can_export_semaphores(device_.physical());
}

std::string SharedSceneHost::validation_text() const {
    std::string text;
    for (const std::string& message : instance_.validation_messages()) {
        text += message + '\n';
    }
    return text;
}

}
