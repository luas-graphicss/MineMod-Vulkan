#pragma once

#include <string>

#include "device/device.hpp"
#include "device/instance.hpp"

namespace minemod::renderer::test_support {

class ValidatedDevice {
public:
    ValidatedDevice();

    const Device& device() const noexcept {
        return device_;
    }

    std::string validation_text() const;

private:
    Instance instance_;
    Device device_;
};

}
