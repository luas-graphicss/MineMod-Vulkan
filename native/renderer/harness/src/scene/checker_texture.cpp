#include "scene/checker_texture.hpp"

#include <cstddef>
#include <vector>

#include "resources/buffer.hpp"
#include "resources/image_layout.hpp"
#include "scene/proof_scene_colors.hpp"

namespace minemod::harness {

namespace {

constexpr VkExtent2D checker_extent{2, 2};

std::vector<std::byte> checker_bytes() {
    std::vector<std::byte> bytes;
    for (const renderer::Pixel& texel : checker_texels) {
        bytes.insert(
            bytes.end(),
            {std::byte{texel.red}, std::byte{texel.green}, std::byte{texel.blue}, std::byte{texel.alpha}});
    }
    return bytes;
}

}

renderer::Image create_checker_texture(const renderer::Device& device) {
    renderer::Image texture(
        device,
        renderer::ImageDescription{
            checker_extent,
            VK_FORMAT_R8G8B8A8_UNORM,
            VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT,
            VK_IMAGE_ASPECT_COLOR_BIT,
        });

    const std::vector<std::byte> texels = checker_bytes();
    const renderer::Buffer staging(device, texels.size(), VK_BUFFER_USAGE_TRANSFER_SRC_BIT, renderer::host_memory);
    staging.write(texels);

    device.submit([&](VkCommandBuffer commands) {
        renderer::change_layout(
            commands, texture.get(), VK_IMAGE_ASPECT_COLOR_BIT, renderer::undefined_to_transfer_destination);
        const VkBufferImageCopy region{
            .imageSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
            .imageExtent = {checker_extent.width, checker_extent.height, 1},
        };
        vkCmdCopyBufferToImage(
            commands, staging.get(), texture.get(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &region);
        renderer::change_layout(
            commands, texture.get(), VK_IMAGE_ASPECT_COLOR_BIT, renderer::transfer_destination_to_sampled);
    });
    return texture;
}

}
