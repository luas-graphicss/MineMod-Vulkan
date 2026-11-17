#include <doctest.h>

#include "device/vulkan_error.hpp"
#include "support/sharing_device.hpp"
#include "support/validated_device.hpp"
#include "sync/shared_semaphore.hpp"

namespace {

using namespace minemod;
using namespace minemod::interop;
using namespace minemod::interop::test_support;

}

TEST_CASE("a device created without the semaphore extension cannot share semaphores") {
    const renderer::test_support::ValidatedDevice gpu;

    CHECK_THROWS_AS(SharedSemaphore(gpu.device()), renderer::UnsupportedSystem);
}

TEST_CASE("semaphores are exported or refused as the device reports") {
    const SharingDevice image_sharing_only;
    if (!can_export_semaphores(image_sharing_only.device().physical())) {
        CHECK_THROWS_AS(SharedSemaphore(image_sharing_only.device()), renderer::UnsupportedSystem);
        return;
    }

    const SharingDevice gpu(shared_semaphore_extensions);
    const SharedSemaphore semaphore(gpu.device());
    CHECK(semaphore.export_handle().is_open());
    CHECK(gpu.validation_text() == "");
}
