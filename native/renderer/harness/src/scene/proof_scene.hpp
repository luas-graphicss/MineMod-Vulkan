#pragma once

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "device/device_handle.hpp"
#include "frame/frame_targets.hpp"
#include "graph/graph_images.hpp"
#include "graph/planned_frame.hpp"
#include "resources/buffer.hpp"
#include "resources/image.hpp"

namespace minemod::harness {

class ProofScene {
public:
    explicit ProofScene(const renderer::Device& device);

    renderer::PlannedFrame plan_frame(const renderer::Device& device, const renderer::FrameTargets& targets) const;

private:
    void record(VkCommandBuffer commands, const renderer::BoundImage& color, const renderer::BoundImage& depth) const;

    renderer::Image texture_;
    renderer::DeviceHandle<VkSampler> sampler_;
    renderer::DeviceHandle<VkDescriptorSetLayout> texture_layout_;
    renderer::DeviceHandle<VkDescriptorPool> descriptor_pool_;
    VkDescriptorSet texture_binding_;
    renderer::DeviceHandle<VkPipelineLayout> pipeline_layout_;
    renderer::DeviceHandle<VkPipeline> pipeline_;
    renderer::Buffer vertices_;
};

}
