#include "textures/texture.hpp"

#include <algorithm>
#include <stdexcept>
#include <vector>

#include "resources/image_layout.hpp"

namespace minemod::renderer {

namespace {

constexpr VkImageUsageFlags texture_usage =
    VK_IMAGE_USAGE_SAMPLED_BIT | VK_IMAGE_USAGE_TRANSFER_DST_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT;

constexpr VkDeviceSize bytes_per_texel = 4;

std::uint32_t level_edge(std::uint32_t base_edge, std::uint32_t level) noexcept {
    return std::max(base_edge >> level, 1u);
}

bool lies_within(const SpriteRegion& region, VkExtent2D extent, std::uint32_t level) noexcept {
    const std::uint32_t width = level_edge(extent.width, level);
    const std::uint32_t height = level_edge(extent.height, level);
    return region.width != 0 && region.height != 0 && region.x < width && region.y < height
        && region.width <= width - region.x && region.height <= height - region.y;
}

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

void Texture::record_region_update(
    VkCommandBuffer commands,
    VkBuffer staging,
    const ArenaRegion& staged,
    std::uint32_t level,
    const SpriteRegion& region) const {
    if (level >= mip_levels_ || !lies_within(region, image_.extent(), level)) {
        throw std::invalid_argument("the updated region lies outside the texture level");
    }
    if (staged.size != VkDeviceSize{region.width} * region.height * bytes_per_texel) {
        throw std::invalid_argument("the staged texels do not match the updated region");
    }

    const VkBufferImageCopy copy{
        .bufferOffset = staged.offset,
        .imageSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, level, 0, 1},
        .imageOffset = {static_cast<std::int32_t>(region.x), static_cast<std::int32_t>(region.y), 0},
        .imageExtent = {region.width, region.height, 1},
    };
    vkCmdCopyBufferToImage(commands, staging, image_.get(), VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL, 1, &copy);
}

}
