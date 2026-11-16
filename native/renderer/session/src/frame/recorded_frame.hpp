#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <optional>
#include <unordered_map>
#include <vector>

#include <vulkan/vulkan.h>

#include "draw/batch_renderer.hpp"
#include "effects/frame_blur.hpp"
#include "frame/batch_options.hpp"
#include "minemod/abi/handle.h"
#include "minemod/abi/scene_commands.h"
#include "terrain/terrain_frame.hpp"
#include "textures/atlas_layout.hpp"
#include "upload/arena_layout.hpp"

namespace minemod::scene {

inline constexpr std::size_t batch_slot_count = MINEMOD_BATCH_INTERFACE + 1;

struct StagedTextureWrite {
    minemod_handle texture;
    std::uint32_t level;
    renderer::SpriteRegion region;
    renderer::ArenaRegion staged;
};

struct BackdropBlur {
    renderer::FrameBlur blur;
    std::size_t interface_batches;
};

struct RecordedFrame {
    void clear() {
        is_open = false;
        draws_world = false;
        uploaded_sections = false;
        clears_depth = false;
        backdrop_blur.reset();
        views.fill(VK_NULL_HANDLE);
        terrain.reset();
        batch_options = BatchOptions{};
        for (std::vector<renderer::DrawBatch>& slot : batches) {
            slot.clear();
        }
        texture_writes.clear();
        unread_textures.clear();
        bound_textures.clear();
    }

    bool is_open = false;
    bool draws_world = false;
    bool uploaded_sections = false;
    bool clears_depth = false;
    std::optional<BackdropBlur> backdrop_blur;
    std::array<float, 4> clear_color{};
    std::array<VkDescriptorSet, MINEMOD_FRAME_MAX_VIEWS> views{};
    std::optional<renderer::TerrainFrame> terrain;
    BatchOptions batch_options;
    std::array<std::vector<renderer::DrawBatch>, batch_slot_count> batches;
    std::vector<StagedTextureWrite> texture_writes;
    std::vector<minemod_handle> unread_textures;
    std::unordered_map<minemod_handle, VkDescriptorSet> bound_textures;
};

}
