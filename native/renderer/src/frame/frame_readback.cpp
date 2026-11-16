#include "frame/frame_readback.hpp"

#include <stdexcept>
#include <utility>

#include "resources/buffer.hpp"

namespace minemod::renderer {

namespace {

constexpr std::size_t bytes_per_pixel = 4;

}

FramePixels::FramePixels(VkExtent2D extent, std::vector<std::byte> rgba) : extent_(extent), rgba_(std::move(rgba)) {
    if (rgba_.size() != std::size_t{extent.width} * extent.height * bytes_per_pixel) {
        throw std::invalid_argument("the pixel bytes do not match the frame extent");
    }
}

Pixel FramePixels::at(std::uint32_t x, std::uint32_t y) const {
    if (x >= extent_.width || y >= extent_.height) {
        throw std::out_of_range("the pixel lies outside the frame");
    }
    const std::size_t first = (std::size_t{y} * extent_.width + x) * bytes_per_pixel;
    return Pixel{
        std::to_integer<std::uint8_t>(rgba_[first]),
        std::to_integer<std::uint8_t>(rgba_[first + 1]),
        std::to_integer<std::uint8_t>(rgba_[first + 2]),
        std::to_integer<std::uint8_t>(rgba_[first + 3]),
    };
}

FramePixels read_back_color(const Device& device, const Image& transfer_source) {
    const VkExtent2D extent = transfer_source.extent();
    const VkDeviceSize byte_count = VkDeviceSize{extent.width} * extent.height * bytes_per_pixel;
    const Buffer staging(device, byte_count, VK_BUFFER_USAGE_TRANSFER_DST_BIT, host_memory);

    device.submit([&](VkCommandBuffer commands) {
        const VkBufferImageCopy region{
            .imageSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
            .imageExtent = {extent.width, extent.height, 1},
        };
        vkCmdCopyImageToBuffer(
            commands, transfer_source.get(), VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL, staging.get(), 1, &region);
    });
    return FramePixels(extent, staging.read());
}

}
