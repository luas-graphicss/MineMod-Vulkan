#include <array>

#include <doctest.h>

#include "device/instance.hpp"
#include "device/physical_device.hpp"
#include "device/vulkan_error.hpp"

namespace {

using namespace minemod::renderer;

constexpr const char* missing_device_extension = "VK_MINEMOD_extension_no_device_has";
constexpr const char* missing_instance_extension = "VK_MINEMOD_extension_no_loader_has";

}

TEST_CASE("a device without a required extension is rejected by name") {
    const Instance instance(Validation::disabled, {});
    const std::array extensions{missing_device_extension};

    CHECK_THROWS_WITH_AS(
        select_device(instance.get(), DeviceRequirements{.extensions = extensions}),
        doctest::Contains(missing_device_extension),
        UnsupportedSystem);
}

TEST_CASE("an instance extension the installation lacks is reported before any instance exists") {
    const std::array extensions{missing_instance_extension};

    CHECK_THROWS_WITH_AS(
        Instance(Validation::disabled, extensions), doctest::Contains(missing_instance_extension), UnsupportedSystem);
}
