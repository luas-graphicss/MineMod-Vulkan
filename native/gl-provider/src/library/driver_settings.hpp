#pragma once

#include <span>
#include <string>

namespace minemod::provider {

struct DriverSetting {
    std::string name;
    std::string value;
};

void apply_driver_settings(std::span<const DriverSetting> settings);

}
