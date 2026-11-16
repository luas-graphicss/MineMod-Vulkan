#include "support/validated_device.hpp"

#include "device/physical_device.hpp"

namespace minemod::renderer::test_support {

ValidatedDevice::ValidatedDevice()
    : instance_(Validation::required, {}), device_(select_device(instance_.get(), DeviceRequirements{}), {}) {}

std::string ValidatedDevice::validation_text() const {
    std::string text;
    for (const std::string& message : instance_.validation_messages()) {
        text += message + '\n';
    }
    return text;
}

}
