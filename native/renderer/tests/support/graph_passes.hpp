#pragma once

#include <vulkan/vulkan.h>

#include "frame/frame_readback.hpp"
#include "graph/render_graph.hpp"

namespace minemod::renderer::test_support {

RecordPass clear_color(ImageId target, Pixel color);

RecordPass clear_color_and_depth(ImageId color_target, ImageId depth_target, Pixel color);

RecordPass copy_color(ImageId source, ImageId destination, VkOffset2D destination_origin);

}
