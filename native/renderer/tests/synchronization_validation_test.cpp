#include <string>

#include <doctest.h>

#include "resources/image.hpp"
#include "resources/image_layout.hpp"
#include "support/validated_device.hpp"

namespace {

using namespace minemod::renderer;
using minemod::renderer::test_support::ValidatedDevice;

constexpr VkExtent2D image_extent{8, 8};

constexpr LayoutChange undefined_to_general_transfer{
    VK_IMAGE_LAYOUT_UNDEFINED,
    VK_IMAGE_LAYOUT_GENERAL,
    VK_PIPELINE_STAGE_2_NONE,
    VK_ACCESS_2_NONE,
    VK_PIPELINE_STAGE_2_ALL_TRANSFER_BIT,
    VK_ACCESS_2_TRANSFER_READ_BIT | VK_ACCESS_2_TRANSFER_WRITE_BIT,
};

Image create_transfer_image(const Device& device) {
    return Image(
        device,
        ImageDescription{
            image_extent,
            VK_FORMAT_R8G8B8A8_UNORM,
            VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
            VK_IMAGE_ASPECT_COLOR_BIT,
        });
}

void clear_then_copy_without_barrier(VkCommandBuffer commands, VkImage source, VkImage destination) {
    const VkClearColorValue color{.float32 = {1.0f, 0.0f, 0.0f, 1.0f}};
    const VkImageSubresourceRange whole_image{VK_IMAGE_ASPECT_COLOR_BIT, 0, 1, 0, 1};
    vkCmdClearColorImage(commands, source, VK_IMAGE_LAYOUT_GENERAL, &color, 1, &whole_image);

    const VkImageCopy region{
        .srcSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
        .dstSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
        .extent = {image_extent.width, image_extent.height, 1},
    };
    vkCmdCopyImage(
        commands, source, VK_IMAGE_LAYOUT_GENERAL, destination, VK_IMAGE_LAYOUT_GENERAL, 1, &region);
}

}

TEST_CASE("required validation reports a read that races the write before it") {
    const ValidatedDevice gpu;
    const Image source = create_transfer_image(gpu.device());
    const Image destination = create_transfer_image(gpu.device());

    gpu.device().submit([&](VkCommandBuffer commands) {
        change_layout(commands, source.get(), VK_IMAGE_ASPECT_COLOR_BIT, undefined_to_general_transfer);
        change_layout(commands, destination.get(), VK_IMAGE_ASPECT_COLOR_BIT, undefined_to_general_transfer);
        clear_then_copy_without_barrier(commands, source.get(), destination.get());
    });

    CHECK(gpu.validation_text().find("SYNC-HAZARD-READ-AFTER-WRITE") != std::string::npos);
}
