#include "support/sharing_device.hpp"

#include "device/physical_device.hpp"
#include "memory/shared_image.hpp"

namespace minemod::interop::test_support {

SharingDevice::SharingDevice() : SharingDevice(shared_image_extensions) {}

SharingDevice::SharingDevice(std::span<const char* const> extensions)
    : instance_(renderer::Validation::required, {}),
      device_(renderer::select_device(instance_.get(), renderer::DeviceRequirements{extensions}), extensions) {}

std::string SharingDevice::validation_text() const {
    std::string text;
    for (const std::string& message : instance_.validation_messages()) {
        text += message + '\n';
    }
    return text;
}

}
