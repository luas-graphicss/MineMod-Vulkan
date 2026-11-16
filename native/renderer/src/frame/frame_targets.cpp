#include "frame/frame_targets.hpp"

namespace minemod::renderer {

FrameTargets::FrameTargets(const Device& device, VkExtent2D extent)
    : color_(
          device,
          ImageDescription{
              extent,
              frame_color_format,
              VK_IMAGE_USAGE_COLOR_ATTACHMENT_BIT | VK_IMAGE_USAGE_TRANSFER_SRC_BIT,
              VK_IMAGE_ASPECT_COLOR_BIT,
          }),
      depth_(
          device,
          ImageDescription{
              extent,
              frame_depth_format,
              VK_IMAGE_USAGE_DEPTH_STENCIL_ATTACHMENT_BIT,
              VK_IMAGE_ASPECT_DEPTH_BIT,
          }) {}

}
