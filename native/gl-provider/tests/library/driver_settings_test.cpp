#include <array>
#include <cstdlib>
#include <string>

#include <doctest.h>

#include "library/driver_settings.hpp"
#include "library/provider_library.hpp"

namespace {

using namespace minemod::provider;

std::string value_the_provider_reads(const char* name) {
    const char* value = std::getenv(name);
    return value != nullptr ? value : "";
}

}

TEST_CASE("driver settings reach the environment the provider reads") {
    const std::array<DriverSetting, 2> settings{
        DriverSetting{"MINEMOD_TEST_DRIVER", "zink"}, DriverSetting{"MINEMOD_TEST_DEVICE_2", "discrete"}};

    apply_driver_settings(settings);

    CHECK(value_the_provider_reads("MINEMOD_TEST_DRIVER") == "zink");
    CHECK(value_the_provider_reads("MINEMOD_TEST_DEVICE_2") == "discrete");
}

TEST_CASE("an empty driver setting leaves no value behind") {
    const std::array<DriverSetting, 1> stale{DriverSetting{"MINEMOD_TEST_CLEARED", "stale"}};
    apply_driver_settings(stale);
    const std::array<DriverSetting, 1> cleared{DriverSetting{"MINEMOD_TEST_CLEARED", ""}};

    apply_driver_settings(cleared);

    CHECK(value_the_provider_reads("MINEMOD_TEST_CLEARED").empty());
}

TEST_CASE("no setting is applied when one of them is not a setting name") {
    for (const char* name : {"", "lowercase", "A=B", "9LIVES", "WITH SPACE"}) {
        const std::array<DriverSetting, 2> settings{
            DriverSetting{"MINEMOD_TEST_UNAPPLIED", "1"}, DriverSetting{name, "1"}};

        CHECK_THROWS_AS(apply_driver_settings(settings), LibraryLoadError);
        CHECK(std::getenv("MINEMOD_TEST_UNAPPLIED") == nullptr);
    }
}
