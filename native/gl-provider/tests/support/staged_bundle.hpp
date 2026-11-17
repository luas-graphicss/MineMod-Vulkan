#pragma once

#include <filesystem>
#include <string>
#include <vector>

#include "library/driver_settings.hpp"

namespace minemod::provider::testing {

struct StagedBundle {
    std::filesystem::path directory;
    std::string entry;
    std::vector<std::string> dependencies;
    std::vector<DriverSetting> driver_settings;
};

StagedBundle read_staged_bundle(const std::filesystem::path& directory);

}
