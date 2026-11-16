#include "scene/proof_scene.hpp"

#include <array>
#include <cstdint>
#include <optional>
#include <span>
#include <utility>

#include "device/vulkan_error.hpp"
#include "graph/image_use.hpp"
#include "graph/render_graph.hpp"
#include "scene/checker_texture.hpp"
#include "scene/proof_geometry.hpp"
#include "scene/proof_pipeline.hpp"
#include "scene/proof_scene_colors.hpp"

namespace minemod::harness {

namespace {

constexpr float farthest_depth = 1.0f;

renderer::DeviceHandle<VkSampler> create_nearest_sampler(const renderer::Device& device) {
    const VkSamplerCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_SAMPLER_CREATE_INFO,
        .magFilter = VK_FILTER_NEAREST,
        .minFilter = VK_FILTER_NEAREST,
        .mipmapMode = VK_SAMPLER_MIPMAP_MODE_NEAREST,
        .addressModeU = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
        .addressModeV = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
        .addressModeW = VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
    };
    return renderer::create_device_handle<VkSampler>(
        device.get(), create_info, vkCreateSampler, vkDestroySampler, "creating the texture sampler");
}

renderer::DeviceHandle<VkDescriptorSetLayout> create_texture_layout(const renderer::Device& device) {
    const VkDescriptorSetLayoutBinding binding{
        .binding = 0,
        .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
        .descriptorCount = 1,
        .stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT,
    };
    const VkDescriptorSetLayoutCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO,
        .bindingCount = 1,
        .pBindings = &binding,
    };
    return renderer::create_device_handle<VkDescriptorSetLayout>(
        device.get(),
        create_info,
        vkCreateDescriptorSetLayout,
        vkDestroyDescriptorSetLayout,
        "creating the texture descriptor layout");
}

renderer::DeviceHandle<VkDescriptorPool> create_descriptor_pool(const renderer::Device& device) {
    const VkDescriptorPoolSize size{VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, 1};
    const VkDescriptorPoolCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_POOL_CREATE_INFO,
        .maxSets = 1,
        .poolSizeCount = 1,
        .pPoolSizes = &size,
    };
    return renderer::create_device_handle<VkDescriptorPool>(
        device.get(), create_info, vkCreateDescriptorPool, vkDestroyDescriptorPool, "creating the descriptor pool");
}

VkDescriptorSet bind_texture(
    const renderer::Device& device,
    VkDescriptorPool pool,
    VkDescriptorSetLayout layout,
    VkImageView texture,
    VkSampler sampler) {
    const VkDescriptorSetAllocateInfo allocation{
        .sType = VK_STRUCTURE_TYPE_DESCRIPTOR_SET_ALLOCATE_INFO,
        .descriptorPool = pool,
        .descriptorSetCount = 1,
        .pSetLayouts = &layout,
    };
    VkDescriptorSet binding = VK_NULL_HANDLE;
    renderer::require_success(
        vkAllocateDescriptorSets(device.get(), &allocation, &binding), "allocating the texture descriptor set");

    const VkDescriptorImageInfo image{
        .sampler = sampler,
        .imageView = texture,
        .imageLayout = VK_IMAGE_LAYOUT_SHADER_READ_ONLY_OPTIMAL,
    };
    const VkWriteDescriptorSet write{
        .sType = VK_STRUCTURE_TYPE_WRITE_DESCRIPTOR_SET,
        .dstSet = binding,
        .dstBinding = 0,
        .descriptorCount = 1,
        .descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER,
        .pImageInfo = &image,
    };
    vkUpdateDescriptorSets(device.get(), 1, &write, 0, nullptr);
    return binding;
}

renderer::DeviceHandle<VkPipelineLayout> create_pipeline_layout(
    const renderer::Device& device, VkDescriptorSetLayout texture_layout) {
    const VkPipelineLayoutCreateInfo create_info{
        .sType = VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO,
        .setLayoutCount = 1,
        .pSetLayouts = &texture_layout,
    };
    return renderer::create_device_handle<VkPipelineLayout>(
        device.get(), create_info, vkCreatePipelineLayout, vkDestroyPipelineLayout, "creating the pipeline layout");
}

renderer::Buffer create_vertex_buffer(const renderer::Device& device) {
    renderer::Buffer vertices(
        device, sizeof(proof_vertices), VK_BUFFER_USAGE_VERTEX_BUFFER_BIT, renderer::host_memory);
    vertices.write(std::as_bytes(std::span(proof_vertices)));
    return vertices;
}

renderer::ImportedImage import_image(
    const renderer::Image& image,
    VkImageAspectFlags aspect,
    const renderer::ImageUse& entry,
    const std::optional<renderer::ImageUse>& exit) {
    return renderer::ImportedImage{image.get(), image.view(), image.extent(), aspect, entry, exit};
}

void begin_rendering(
    VkCommandBuffer commands, const renderer::BoundImage& color_target, const renderer::BoundImage& depth_target) {
    const std::array<float, 4> clear_color = unit_color(proof_clear_color);
    const VkRenderingAttachmentInfo color{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = color_target.view,
        .imageLayout = VK_IMAGE_LAYOUT_COLOR_ATTACHMENT_OPTIMAL,
        .loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR,
        .storeOp = VK_ATTACHMENT_STORE_OP_STORE,
        .clearValue = {.color = {.float32 = {clear_color[0], clear_color[1], clear_color[2], clear_color[3]}}},
    };
    const VkRenderingAttachmentInfo depth{
        .sType = VK_STRUCTURE_TYPE_RENDERING_ATTACHMENT_INFO,
        .imageView = depth_target.view,
        .imageLayout = VK_IMAGE_LAYOUT_DEPTH_STENCIL_ATTACHMENT_OPTIMAL,
        .loadOp = VK_ATTACHMENT_LOAD_OP_CLEAR,
        .storeOp = VK_ATTACHMENT_STORE_OP_DONT_CARE,
        .clearValue = {.depthStencil = {farthest_depth, 0}},
    };
    const VkRenderingInfo rendering{
        .sType = VK_STRUCTURE_TYPE_RENDERING_INFO,
        .renderArea = {{0, 0}, color_target.extent},
        .layerCount = 1,
        .colorAttachmentCount = 1,
        .pColorAttachments = &color,
        .pDepthAttachment = &depth,
    };
    vkCmdBeginRendering(commands, &rendering);
}

}

ProofScene::ProofScene(const renderer::Device& device)
    : texture_(create_checker_texture(device)),
      sampler_(create_nearest_sampler(device)),
      texture_layout_(create_texture_layout(device)),
      descriptor_pool_(create_descriptor_pool(device)),
      texture_binding_(
          bind_texture(device, descriptor_pool_.get(), texture_layout_.get(), texture_.view(), sampler_.get())),
      pipeline_layout_(create_pipeline_layout(device, texture_layout_.get())),
      pipeline_(create_proof_pipeline(device, pipeline_layout_.get())),
      vertices_(create_vertex_buffer(device)) {}

renderer::PlannedFrame ProofScene::plan_frame(
    const renderer::Device& device, const renderer::FrameTargets& targets) const {
    renderer::RenderGraph graph;
    const renderer::ImageId color = graph.import_image(
        "frame color",
        import_image(
            targets.color(), VK_IMAGE_ASPECT_COLOR_BIT, renderer::uses::untouched, renderer::uses::transfer_source));
    const renderer::ImageId depth = graph.import_image(
        "frame depth", import_image(targets.depth(), VK_IMAGE_ASPECT_DEPTH_BIT, renderer::uses::untouched, std::nullopt));
    const renderer::ImageId texture = graph.import_image(
        "checker texture",
        import_image(texture_, VK_IMAGE_ASPECT_COLOR_BIT, renderer::uses::sampled_by_fragment_shader, std::nullopt));

    graph.add_pass(
        "proof scene",
        {
            {color, renderer::uses::color_attachment},
            {depth, renderer::uses::depth_attachment},
            {texture, renderer::uses::sampled_by_fragment_shader},
        },
        [this, color, depth](VkCommandBuffer commands, const renderer::GraphImages& images) {
            record(commands, images.at(color), images.at(depth));
        });
    return renderer::PlannedFrame(device, std::move(graph));
}

void ProofScene::record(
    VkCommandBuffer commands, const renderer::BoundImage& color, const renderer::BoundImage& depth) const {
    begin_rendering(commands, color, depth);

    const VkExtent2D extent = color.extent;
    const VkViewport viewport{
        0.0f, 0.0f, static_cast<float>(extent.width), static_cast<float>(extent.height), 0.0f, farthest_depth};
    const VkRect2D scissor{{0, 0}, extent};
    vkCmdSetViewport(commands, 0, 1, &viewport);
    vkCmdSetScissor(commands, 0, 1, &scissor);

    vkCmdBindPipeline(commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline_.get());
    vkCmdBindDescriptorSets(
        commands, VK_PIPELINE_BIND_POINT_GRAPHICS, pipeline_layout_.get(), 0, 1, &texture_binding_, 0, nullptr);
    const VkBuffer vertex_buffer = vertices_.get();
    const VkDeviceSize first_byte = 0;
    vkCmdBindVertexBuffers(commands, 0, 1, &vertex_buffer, &first_byte);
    vkCmdDraw(commands, static_cast<std::uint32_t>(proof_vertices.size()), 1, 0, 0);

    vkCmdEndRendering(commands);
}

}
