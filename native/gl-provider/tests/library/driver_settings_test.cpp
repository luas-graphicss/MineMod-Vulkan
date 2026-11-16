#include <array>
#include <cstdlib>
#include <string>

#include <doctest.h>

#include "library/driver_settings.hpp"
#include "library/provider_library.hpp"

namespace {

using namespace minemod::provider;

}

TEST_CASE("driver settings reach the environment the provider reads") {
    const std::array<DriverSetting, 2> settings{
        DriverSetting{"MINEMOD_TEST_DRIVER", "zink"}, DriverSetting{"MINEMOD_TEST_DEVICE_2", ""}};

    apply_driver_settings(settings);

    CHECK(std::string(std::getenv("MINEMOD_TEST_DRIVER")) == "zink");
    CHECK(std::string(std::getenv("MINEMOD_TEST_DEVICE_2")).empty());
}

TEST_CASE("no setting is applied when one of them is not a setting name") {
    for (const char* name : {"", "lowercase", "A=B", "9LIVES", "WITH SPACE"}) {
        const std::array<DriverSetting, 2> settings{
            DriverSetting{"MINEMOD_TEST_UNAPPLIED", "1"}, DriverSetting{name, "1"}};

        CHECK_THROWS_AS(apply_driver_settings(settings), LibraryLoadError);
        CHECK(std::getenv("MINEMOD_TEST_UNAPPLIED") == nullptr);
    }
}
