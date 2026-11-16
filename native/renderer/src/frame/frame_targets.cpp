#include "frame/frame_targets.hpp"

namespace minemod::renderer {

namespace {

constexpr VkImageUsageFlags filtered_color_usage = VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_SAMPLED_BIT;

ImageDescription color_image(VkExtent2D extent, VkImageUsageFlags usage) {
    return ImageDescription{extent, frame_color_format, usage, VK_IMAGE_ASPECT_COLOR_BIT};
}

}

FrameTargets::FrameTargets(const Device& device, VkExtent2D extent)
    : color_(device, color_image(extent, filtered_color_usage | VK_IMAGE_USAGE_TRANSFER_SRC_BIT)),
      depth_(
          device,
          ImageDescription{
              extent,
              frame_depth_format,
              VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
              VK_IMAGE_ASPECT_DEPTH_BIT,
          }),
      scratch_(device, color_image(extent, filtered_color_usage)) {}

}
