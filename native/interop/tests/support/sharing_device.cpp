#include "support/sharing_device.hpp"

#include "device/physical_device.hpp"
#include "memory/shared_image.hpp"

namespace minemod::interop::test_support {

SharingDevice::SharingDevice()
    : instance_(renderer::Validation::required, {}),
      device_(
          renderer::select_device(instance_.get(), renderer::DeviceRequirements{shared_image_extensions}),
          shared_image_extensions) {}

std::string SharingDevice::validation_text() const {
    std::string text;
    for (const std::string& message : instance_.validation_messages()) {
        text += message + '\n';
    }
    return text;
}

}
