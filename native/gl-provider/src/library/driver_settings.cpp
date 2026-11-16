#include "library/driver_settings.hpp"

#include <algorithm>
#include <cstdlib>

#include "library/provider_library.hpp"

namespace minemod::provider {

namespace {

bool is_setting_character(char character) noexcept {
    return (character >= 'A' && character <= 'Z') || (character >= '0' && character <= '9') || character == '_';
}

bool is_setting_name(const std::string& name) noexcept {
    return !name.empty() && (name.front() < '0' || name.front() > '9')
        && std::all_of(name.begin(), name.end(), &is_setting_character);
}

}

void apply_driver_settings(std::span<const DriverSetting> settings) {
    for (const DriverSetting& setting : settings) {
        if (!is_setting_name(setting.name)) {
            throw LibraryLoadError("a driver setting is named in capitals, digits and underscores: " + setting.name);
        }
    }
    for (const DriverSetting& setting : settings) {
        if (::setenv(setting.name.c_str(), setting.value.c_str(), 1) != 0) {
            throw LibraryLoadError("the driver setting " + setting.name + " could not be applied");
        }
    }
}

}
