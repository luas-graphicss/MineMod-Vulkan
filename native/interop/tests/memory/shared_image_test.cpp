#include <cstddef>
#include <cstdint>
#include <limits>

#include <doctest.h>

#include "device/vulkan_error.hpp"
#include "frame/frame_targets.hpp"
#include "memory/external_support.hpp"
#include "memory/shared_image.hpp"
#include "support/descriptor_count.hpp"
#include "support/sharing_device.hpp"
#include "support/validated_device.hpp"

namespace {

using namespace minemod;
using namespace minemod::interop;
using namespace minemod::interop::test_support;

constexpr VkExtent2D target_extent{64, 64};
constexpr VkDeviceSize color_bytes = VkDeviceSize{target_extent.width} * target_extent.height * 4;
constexpr int export_cycles = 200;
constexpr std::uint32_t full_chain_levels = 7;

constexpr SharedImageDescription color_target{
    target_extent,
    renderer::frame_color_format,
    VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT,
};
constexpr SharedImageDescription depth_target{
    target_extent,
    renderer::frame_depth_format,
    VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
};
constexpr SharedImageDescription compressed_attachment{
    target_extent,
    VK_FORMAT_BC1_RGB_UNORM_BLOCK,
    VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT,
};
constexpr SharedImageDescription oversized_target{
    {std::numeric_limits<std::uint32_t>::max(), target_extent.height},
    color_target.format,
    color_target.usage,
};
constexpr SharedImageDescription mipped_texture{
    target_extent,
    renderer::frame_color_format,
    VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT,
    full_chain_levels,
};
constexpr SharedImageDescription overlong_chain{
    target_extent,
    mipped_texture.format,
    mipped_texture.usage,
    full_chain_levels + 1,
};
constexpr SharedImageDescription without_levels{
    target_extent,
    mipped_texture.format,
    mipped_texture.usage,
    0,
};

}

TEST_CASE("the frame color format is exportable and a format that is no attachment is not") {
    const SharingDevice gpu;
    const VkPhysicalDevice device = gpu.device().physical();

    CHECK(query_external_support(device, color_target.format, color_target.usage).exportable);
    CHECK_FALSE(
        query_external_support(device, compressed_attachment.format, compressed_attachment.usage).exportable);
}

TEST_CASE("an image that cannot be exported or does not fit the device is rejected by name") {
    const SharingDevice gpu;

    CHECK_THROWS_AS(SharedImage(gpu.device(), compressed_attachment), renderer::UnsupportedSystem);
    CHECK_THROWS_AS(SharedImage(gpu.device(), oversized_target), renderer::UnsupportedSystem);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a device created without the sharing extension cannot share images") {
    const renderer::test_support::ValidatedDevice gpu;

    CHECK_THROWS_AS(SharedImage(gpu.device(), color_target), renderer::UnsupportedSystem);
}

TEST_CASE("exported memory is a descriptor owned by the caller and sized for the image") {
    const SharingDevice gpu;
    const SharedImage image(gpu.device(), color_target);

    const OwnedDescriptor memory = image.export_memory();

    CHECK(memory.is_open());
    CHECK(image.allocation_size() >= color_bytes);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a shared texture keeps every level of its mip chain") {
    const SharingDevice gpu;
    const SharedImage single_level(gpu.device(), {mipped_texture.extent, mipped_texture.format, mipped_texture.usage});
    const SharedImage texture(gpu.device(), mipped_texture);

    CHECK(texture.export_memory().is_open());
    CHECK(texture.allocation_size() > single_level.allocation_size());
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("a mip chain that the extent cannot have is rejected by name") {
    const SharingDevice gpu;

    CHECK_THROWS_AS(SharedImage(gpu.device(), overlong_chain), renderer::UnsupportedSystem);
    CHECK_THROWS_AS(SharedImage(gpu.device(), without_levels), renderer::UnsupportedSystem);
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("depth attachments are exported or refused as the device reports") {
    const SharingDevice gpu;
    const ExternalSupport support =
        query_external_support(gpu.device().physical(), depth_target.format, depth_target.usage);

    if (!support.exportable) {
        CHECK_THROWS_AS(SharedImage(gpu.device(), depth_target), renderer::UnsupportedSystem);
        return;
    }

    const SharedImage image(gpu.device(), depth_target);
    CHECK(image.export_memory().is_open());
    CHECK(gpu.validation_text() == "");
}

TEST_CASE("repeated exports and destructions leave no descriptor behind") {
    const SharingDevice gpu;
    const std::ptrdiff_t open_before = open_descriptor_count();

    for (int cycle = 0; cycle < export_cycles; ++cycle) {
        const SharedImage image(gpu.device(), color_target);
        const OwnedDescriptor memory = image.export_memory();
        REQUIRE(memory.is_open());
    }

    CHECK(open_descriptor_count() == open_before);
    CHECK(gpu.validation_text() == "");
}
