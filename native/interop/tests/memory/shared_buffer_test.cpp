#include <cstddef>
#include <stdexcept>

#include <doctest.h>

#include "device/vulkan_error.hpp"
#include "memory/shared_buffer.hpp"
#include "support/open_handles.hpp"
#include "support/sharing_device.hpp"
#include "support/validated_device.hpp"

namespace {

using namespace minemod;
using namespace minemod::interop;
using namespace minemod::interop::test_support;

constexpr int export_cycles = 200;

constexpr SharedBufferDescription vertex_data{
    4096,
    VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
};
constexpr SharedBufferDescription without_bytes{0, vertex_data.usage};

}

TEST_CASE("exported buffer memory is a handle owned by the caller and sized for the buffer") {
    const SharingDevice gpu;
    REQUIRE(can_export_buffers(gpu.device().physical(), vertex_data.usage));
    const SharedBuffer buffer(gpu.device(), vertex_data);

    const ExportedHandle memory = buffer.export_memory();

    CHECK(memory.is_open());
    CHECK(buffer.allocation_size() >= vertex_data.size_bytes);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a buffer without bytes is rejected before anything is created") {
    const SharingDevice gpu;

    CHECK_THROWS_AS(SharedBuffer(gpu.device(), without_bytes), std::invalid_argument);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a device created without the sharing extension cannot share buffers") {
    const renderer::test_support::ValidatedDevice gpu;

    CHECK_THROWS_AS(SharedBuffer(gpu.device(), vertex_data), renderer::UnsupportedSystem);
}

TEST_CASE("repeated buffer exports and destructions leave no handle behind") {
    const SharingDevice gpu;
    const std::ptrdiff_t open_before = open_handle_count();

    for (int cycle = 0; cycle < export_cycles; ++cycle) {
        const SharedBuffer buffer(gpu.device(), vertex_data);
        const ExportedHandle memory = buffer.export_memory();
        REQUIRE(memory.is_open());
    }

    CHECK(open_handle_count() == open_before);
    CHECK(gpu.validation_text() == "");
}
