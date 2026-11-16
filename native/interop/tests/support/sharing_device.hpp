#pragma once

#include <string>

#include "device/device.hpp"
#include "device/instance.hpp"

namespace minemod::interop::test_support {

class SharingDevice {
public:
    SharingDevice();

    const renderer::Device& device() const noexcept {
        return device_;
    }

    std::string validation_text() const;

private:
    renderer::Instance instance_;
    renderer::Device device_;
};

}
