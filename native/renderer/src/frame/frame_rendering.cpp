#include "frame/frame_rendering.hpp"

namespace minemod::renderer {
namespace {

constexpr float farthest_depth = 1.0f;

VkRenderingAttachmentInfo color_attachment(
    const BoundImage& target, const std::optional<std::array<float, 4>>& clear_color) {
    VkRenderingAttachmentInfo attachment{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = target.view,
        .imageLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
        .loadOp = VK_ATTACHMENT_LOAD_OP_LOAD,
        .storeOp = VK_ATTACHMENT_STORE_OP_STORE,
    };
    if (clear_color) {
        attachment.loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR;
        attachment.clearValue.color = {
            .float32 = {(*clear_color)[0], (*clear_color)[1], (*clear_color)[2], (*clear_color)[3]}};
    }
    return attachment;
}

}

void begin_frame_rendering(
    VkCommandBuffer commands,
    const FrameAttachments& attachments,
    const std::optional<std::array<float, 4>>& clear_color,
    const DepthOperations& depth_operations) {
    const VkRenderingAttachmentInfo color = color_attachment(attachments.color, clear_color);
    const VkRenderingAttachmentInfo depth{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = attachments.depth.view,
        .imageLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
        .loadOp = depth_operations.load,
        .storeOp = depth_operations.store,
        .clearValue = {.depthStencil = {farthest_depth, 0}},
    };
    const VkRenderingInfo rendering{
        .sType = VK_STRUCTURE_TYPE_RENDERING_INFO,
        .renderArea = {{0, 0}, attachments.color.extent},
        .layerCount = 1,
        .colorAttachmentCount = 1,
        .pColorAttachments = &color,
        .pDepthAttachment = &depth,
    };
    vkCmdBeginRendering(commands, &rendering);
}

void clear_frame_depth(VkCommandBuffer commands, VkExtent2D extent) {
    const VkClearAttachment depth{
        .aspectMask = VK_IMAGE_ASPECT_DEPTH_BIT,
        .clearValue = {.depthStencil = {farthest_depth, 0}},
    };
    const VkClearRect whole_frame{{{0, 0}, extent}, 0, 1};
    vkCmdClearAttachments(commands, 1, &depth, 1, &whole_frame);
}

}
