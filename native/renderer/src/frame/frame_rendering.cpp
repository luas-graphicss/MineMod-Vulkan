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
    const std::optional<std::array<float, 4>>& clear_color) {
    const VkRenderingAttachmentInfo color = color_attachment(attachments.color, clear_color);
    const VkRenderingAttachmentInfo depth{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = attachments.depth.view,
        .imageLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
        .loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR,
        .storeOp = VK_ATTACHMENT_STORE_OP_DONT_CARE,
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

}
