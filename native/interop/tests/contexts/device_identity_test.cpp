#include <cstdint>

#include <doctest.h>

#include "contexts/device_identity.hpp"
#include "device/vulkan_error.hpp"
#include "support/sharing_device.hpp"

namespace {

using namespace minemod;
using namespace minemod::interop;
using namespace minemod::interop::test_support;

std::uint8_t inverted(std::uint8_t byte) {
    return static_cast<std::uint8_t>(~byte);
}

}

TEST_CASE("a device answers with the same identity every time") {
    const SharingDevice gpu;

    CHECK(identify_device(gpu.device().physical()) == identify_device(gpu.device().physical()));
}

TEST_CASE("sharing is refused when the provider runs on another GPU or another driver") {
    const SharingDevice gpu;
    const DeviceIdentity renderer_device = identify_device(gpu.device().physical());
    DeviceIdentity other_gpu = renderer_device;
    other_gpu.device_uuid.front() = inverted(other_gpu.device_uuid.front());
    DeviceIdentity other_driver = renderer_device;
    other_driver.driver_uuid.back() = inverted(other_driver.driver_uuid.back());

    CHECK_NOTHROW(require_same_gpu(renderer_device, renderer_device));
    CHECK_THROWS_AS(require_same_gpu(renderer_device, other_gpu), renderer::UnsupportedSystem);
    CHECK_THROWS_AS(require_same_gpu(renderer_device, other_driver), renderer::UnsupportedSystem);
}
