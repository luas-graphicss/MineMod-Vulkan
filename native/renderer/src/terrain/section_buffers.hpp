#pragma once

#include <array>
#include <optional>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "resources/buffer.hpp"
#include "terrain/section_mesh.hpp"
#include "terrain/terrain_layer.hpp"
#include "upload/arena_layout.hpp"
#include "upload/upload_arena.hpp"

namespace minemod::renderer {

struct StagedSection {
    ArenaRegion vertices;
    ArenaRegion indices;
};

std::optional<StagedSection> stage_section(UploadArena& arena, const SectionMesh& mesh);

class SectionBuffers {
public:
    SectionBuffers(const Device& device, const SectionMesh& mesh);

    void record_upload(VkCommandBuffer commands, VkBuffer staging, const StagedSection& staged) const;
    void record_translucent_order(VkCommandBuffer commands, VkBuffer staging, const ArenaRegion& staged) const;

    VkBuffer vertices() const noexcept {
        return vertices_.get();
    }

    VkBuffer indices() const noexcept {
        return indices_.get();
    }

    const IndexRange& range(TerrainLayer layer) const noexcept {
        return layers_[layer_index(layer)];
    }

    VkDeviceSize size_bytes() const noexcept {
        return vertex_bytes_ + index_bytes_;
    }

private:
    VkDeviceSize vertex_bytes_;
    VkDeviceSize index_bytes_;
    Buffer vertices_;
    Buffer indices_;
    std::array<IndexRange, terrain_layer_count> layers_;
};

}
