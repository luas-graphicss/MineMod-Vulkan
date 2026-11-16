#include "support/graph_passes.hpp"

#include "graph/graph_images.hpp"

namespace minemod::renderer::test_support {

namespace {

constexpr float farthest_depth = 1.0f;
constexpr float brightest_channel = 255.0f;

VkRenderingAttachmentInfo cleared_color_attachment(const BoundImage& target, Pixel color) {
    return VkRenderingAttachmentInfo{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = target.view,
        .imageLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
        .loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR,
        .storeOp = VK_ATTACHMENT_STORE_OP_STORE,
        .clearValue = {.color = {.float32 = {
            static_cast<float>(color.red) / brightest_channel,
            static_cast<float>(color.green) / brightest_channel,
            static_cast<float>(color.blue) / brightest_channel,
            static_cast<float>(color.alpha) / brightest_channel,
        }}},
    };
}

VkRenderingAttachmentInfo cleared_depth_attachment(const BoundImage& target) {
    return VkRenderingAttachmentInfo{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = target.view,
        .imageLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
        .loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR,
        .storeOp = VK_ATTACHMENT_STORE_OP_DONT_CARE,
        .clearValue = {.depthStencil = {farthest_depth, 0}},
    };
}

void clear_attachments(
    VkCommandBuffer commands,
    VkExtent2D extent,
    const VkRenderingAttachmentInfo& color,
    const VkRenderingAttachmentInfo* depth) {
    const VkRenderingInfo rendering{
        .sType = VK_STRUCTURE_TYPE_RENDERING_INFO,
        .renderArea = {{0, 0}, extent},
        .layerCount = 1,
        .colorAttachmentCount = 1,
        .pColorAttachments = &color,
        .pDepthAttachment = depth,
    };
    vkCmdBeginRendering(commands, &rendering);
    vkCmdEndRendering(commands);
}

}

RecordPass clear_color(ImageId target, Pixel color) {
    return [=](VkCommandBuffer commands, const GraphImages& images) {
        const BoundImage& bound = images.at(target);
        clear_attachments(commands, bound.extent, cleared_color_attachment(bound, color), nullptr);
    };
}

RecordPass clear_color_and_depth(ImageId color_target, ImageId depth_target, Pixel color) {
    return [=](VkCommandBuffer commands, const GraphImages& images) {
        const BoundImage& bound = images.at(color_target);
        const VkRenderingAttachmentInfo depth = cleared_depth_attachment(images.at(depth_target));
        clear_attachments(commands, bound.extent, cleared_color_attachment(bound, color), &depth);
    };
}

RecordPass copy_color(ImageId source, ImageId destination, VkOffset2D destination_origin) {
    return [=](VkCommandBuffer commands, const GraphImages& images) {
        const BoundImage& from = images.at(source);
        const VkImageCopy region{
            .srcSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
            .dstSubresource = {VK_IMAGE_ASPECT_COLOR_BIT, 0, 0, 1},
            .dstOffset = {destination_origin.x, destination_origin.y, 0},
            .extent = {from.extent.width, from.extent.height, 1},
        };
        vkCmdCopyImage(
            commands,
            from.handle,
            VK_IMAGE_LAYOUT_TRANSFER_SRC_OPTIMAL,
            images.at(destination).handle,
            VK_IMAGE_LAYOUT_TRANSFER_DST_OPTIMAL,
            1,
            &region);
    };
}

}
