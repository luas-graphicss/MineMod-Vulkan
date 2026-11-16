#include "frame/frame_slot.hpp"

#include <cstddef>

#include "minemod/abi/scene_commands.h"

namespace minemod::scene {

namespace {

constexpr std::uint32_t sets_per_pool = 256;
constexpr std::uint32_t constant_blocks_per_pool = 64;
constexpr std::uint32_t sampled_images_per_pool = 512;
constexpr std::size_t max_descriptor_pools = 64;

std::vector<renderer::ViewConstants> create_views(const renderer::Device& device) {
    std::vector<renderer::ViewConstants> views;
    views.reserve(MINEMOD_FRAME_MAX_VIEWS);
    for (std::uint32_t view = 0; view < MINEMOD_FRAME_MAX_VIEWS; ++view) {
        views.emplace_back(device);
    }
    return views;
}

}

FrameSlot::FrameSlot(const renderer::Device& device, const SceneLimits& limits)
    : uploads(device, limits.upload_bytes_per_frame),
      geometry(device, limits.geometry_bytes_per_frame, renderer::geometry_arena_usage),
      stager(geometry),
      views(create_views(device)),
      descriptors(
          device,
          renderer::DescriptorPoolShape{
              sets_per_pool,
              {
                  {VK_DESCRIPTOR_TYPE_UNIFORM_BUFFER, constant_blocks_per_pool},
                  {VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER, sampled_images_per_pool},
              },
              max_descriptor_pools,
          }) {}

void FrameSlot::reuse() {
    planned.reset();
    uploads.reset();
    geometry.reset();
    descriptors.reset();
}

}
