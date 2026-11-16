#include "support/presenting_device.hpp"

#include <array>

#include "device/physical_device.hpp"

namespace minemod::renderer::test_support {

namespace {

constexpr std::array device_extensions{VK_KHR_SWAPCHAIN_EXTENSION_NAME};

}

PresentingDevice::PresentingDevice()
    : instance_(Validation::required, headless_surface_extensions),
      surface_(instance_.get()),
      device_(
          select_device(
              instance_.get(),
              DeviceRequirements{.extensions = device_extensions, .presentation_surface = surface_.get()}),
          device_extensions) {}

std::string PresentingDevice::validation_text() const {
    std::string text;
    for (const std::string& message : instance_.validation_messages()) {
        text += message + '\n';
    }
    return text;
}

}
