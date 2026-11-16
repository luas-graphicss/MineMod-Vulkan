#include "textures/texture.hpp"

#include <stdexcept>
#include <vector>

#include "resources/image_layout.hpp"

namespace minemod::renderer {

namespace {

constexpr VkImageUsageFlags texture_usage =
    VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT;

bool covers(const MipChain& chain, VkExtent2D extent, std::uint32_t mip_levels) {
    return chain.levels.size() == mip_levels && chain.levels.front().extent.width == extent.width
        && chain.levels.front().extent.height == extent.height;
}

}

Texture::Texture(const Device& device, VkExtent2D extent, std::uint32_t mip_levels)
    : image_(device, ImageDescription{extent, texture_format, texture_usage, VK_IMAGE_ASPECT_COLOR_BIT, mip_levels}),
      mip_levels_(mip_levels) {}

void Texture::record_upload(
    VkCommandBuffer commands, VkBuffer staging, const ArenaRegion& staged, const MipChain& chain) const {
    if (!covers(chain, image_.extent(), mip_levels_) || staged.size != chain.rgba.size()) {
        throw std::invalid_argument("the staged mip chain does not match the texture");
    }

    std::vector<VkBufferImageCopy> copies;
    copies.reserve(chain.levels.size());
    for (std::uint32_t level = 0; level < mip_levels_; ++level) {
        const MipLevel& mip = chain.levels[level];
        copies.push_back(VkBufferImageCopy{
            .bufferOffset = staged.offset + mip.offset,
            .imageSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, level, 0, 1},
            .imageExtent = {mip.extent.width, mip.extent.height, 1},
        });
    }

    change_layout(commands, image_.get(), VK_IMAGE_ASPECT_COLOR_BIT, undefined_to_transfer_destination);
    vkCmdCopyBufferToImage(
        commands,
        staging,
        image_.get(),
        VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
        static_cast<std::uint32_t>(copies.size()),
        copies.data());
    change_layout(commands, image_.get(), VK_IMAGE_ASPECT_COLOR_BIT, transfer_destination_to_sampled);
}

}
