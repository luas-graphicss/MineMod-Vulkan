#include "support/staged_bundle.hpp"

#include <fstream>
#include <iterator>
#include <sstream>
#include <stdexcept>
#include <string_view>

namespace minemod::provider::testing {

namespace {

constexpr const char* manifest_file_name = "provider.properties";
constexpr std::string_view entry_key = "entry";
constexpr std::string_view dependencies_key = "dependencies";
constexpr std::string_view driver_setting_prefix = "driver.";

std::vector<std::string> words_of(const std::string& text) {
    std::istringstream words(text);
    return {std::istream_iterator<std::string>(words), std::istream_iterator<std::string>()};
}

}

StagedBundle read_staged_bundle(const std::filesystem::path& directory) {
    const std::filesystem::path manifest_file = directory / manifest_file_name;
    std::ifstream manifest(manifest_file);
    if (!manifest) {
        throw std::runtime_error(manifest_file.string() + " cannot be read");
    }

    StagedBundle bundle{directory, {}, {}, {}};
    for (std::string line; std::getline(manifest, line);) {
        const std::size_t separator = line.find('=');
        if (separator == std::string::npos) {
            throw std::runtime_error(manifest_file.string() + " has a line without a value: " + line);
        }
        const std::string_view key = std::string_view(line).substr(0, separator);
        const std::string value = line.substr(separator + 1);
        if (key == entry_key) {
            bundle.entry = value;
        } else if (key == dependencies_key) {
            bundle.dependencies = words_of(value);
        } else if (key.starts_with(driver_setting_prefix)) {
            bundle.driver_settings.push_back({std::string(key.substr(driver_setting_prefix.size())), value});
        }
    }
    if (bundle.entry.empty()) {
        throw std::runtime_error(manifest_file.string() + " names no entry library");
    }
    return bundle;
}

}
