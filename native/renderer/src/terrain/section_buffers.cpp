#include "terrain/section_buffers.hpp"

#include <cstddef>
#include <span>
#include <stdexcept>

namespace minemod::renderer {
namespace {

constexpr VkDeviceSize staging_alignment = 4;

const SectionMesh& drawable(const SectionMesh& mesh) {
    require_drawable(mesh);
    return mesh;
}

void copy_region(VkCommandBuffer commands, VkBuffer staging, const ArenaRegion& staged, VkBuffer destination) {
    const VkBufferCopy copy{staged.offset, 0, staged.size};
    vkCmdCopyBuffer(commands, staging, destination, 1, &copy);
}

}

std::optional<StagedSection> stage_section(UploadArena& arena, const SectionMesh& mesh) {
    require_drawable(mesh);

    const std::optional<ArenaRegion> vertices = arena.stage(std::as_bytes(std::span(mesh.vertices)), staging_alignment);
    if (!vertices) {
        return std::nullopt;
    }
    const std::optional<ArenaRegion> indices = arena.stage(std::as_bytes(std::span(mesh.indices)), staging_alignment);
    if (!indices) {
        return std::nullopt;
    }
    return StagedSection{*vertices, *indices};
}

SectionBuffers::SectionBuffers(const Device& device, const SectionMesh& mesh)
    : vertex_bytes_(drawable(mesh).vertices.size() * sizeof(TerrainVertex)),
      index_bytes_(mesh.indices.size() * sizeof(std::uint32_t)),
      vertices_(
          device,
          vertex_bytes_,
          VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
          VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
      indices_(
          device,
          index_bytes_,
          VK_BUFFER_USAGE_INDEX_BUFFER_BIT | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
          VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT),
      layers_(mesh.layers) {}

void SectionBuffers::record_upload(VkCommandBuffer commands, VkBuffer staging, const StagedSection& staged) const {
    if (staged.vertices.size != vertex_bytes_ || staged.indices.size != index_bytes_) {
        throw std::invalid_argument("the staged section does not match the mesh its buffers were sized for");
    }
    copy_region(commands, staging, staged.vertices, vertices_.get());
    copy_region(commands, staging, staged.indices, indices_.get());
}

void SectionBuffers::record_translucent_order(
    VkCommandBuffer commands, VkBuffer staging, const ArenaRegion& staged) const {
    const IndexRange& translucent = range(TerrainLayer::translucent);
    const VkDeviceSize order_bytes = VkDeviceSize{translucent.index_count} * sizeof(std::uint32_t);
    if (order_bytes == 0 || staged.size != order_bytes) {
        throw std::invalid_argument("the staged order does not match the translucent indices of the section");
    }
    const VkBufferCopy copy{staged.offset, VkDeviceSize{translucent.first_index} * sizeof(std::uint32_t), order_bytes};
    vkCmdCopyBuffer(commands, staging, indices_.get(), 1, &copy);
}

}
