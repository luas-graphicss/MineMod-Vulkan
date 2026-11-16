#pragma once

#include <cstdint>
#include <memory>
#include <vector>

#include <vulkan/vulkan.h>

#include "descriptors/descriptor_pools.hpp"
#include "device/device.hpp"
#include "draw/batch_geometry.hpp"
#include "draw/view_constants.hpp"
#include "graph/planned_frame.hpp"
#include "upload/upload_arena.hpp"

namespace minemod::scene {

struct SceneLimits {
    VkDeviceSize upload_bytes_per_frame;
    VkDeviceSize geometry_bytes_per_frame;
    std::uint32_t max_textures;
    std::uint32_t max_sections;
};

struct FrameSlot {
    FrameSlot(const renderer::Device& device, const SceneLimits& limits);

    FrameSlot(const FrameSlot&) = delete;
    FrameSlot& operator=(const FrameSlot&) = delete;

    void reuse();

    renderer::UploadArena uploads;
    renderer::UploadArena geometry;
    renderer::BatchStager stager;
    std::vector<renderer::ViewConstants> views;
    renderer::DescriptorPools descriptors;
    std::unique_ptr<renderer::PlannedFrame> planned;
};

}
