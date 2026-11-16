#include "draw/static_mesh.hpp"

#include <stdexcept>

namespace minemod::renderer {
namespace {

constexpr VkDeviceSize index_alignment = 4;

constexpr VkDeviceSize aligned_for_indices(VkDeviceSize bytes) noexcept {
    return (bytes + index_alignment - 1) / index_alignment * index_alignment;
}

}

StaticMesh::StaticMesh(const Device& device, const BatchGeometry& staged)
    : vertex_bytes_(staged.vertices.size),
      index_bytes_(staged.indices.size),
      index_offset_(aligned_for_indices(staged.vertices.size)),
      index_count_(staged.index_count),
      buffer_(
          device,
          index_offset_ + index_bytes_,
          geometry_arena_usage | VK_BUFFER_USAGE_TRANSFER_DST_BIT,
          VK_MEMORY_PROPERTY_DEVICE_LOCAL_BIT) {}

void StaticMesh::record_upload(VkCommandBuffer commands, const BatchGeometry& staged) const {
    if (staged.vertices.size != vertex_bytes_ || staged.indices.size != index_bytes_) {
        throw std::invalid_argument("the staged geometry does not match the mesh its buffer was sized for");
    }
    const VkBufferCopy vertices{staged.vertices.offset, 0, vertex_bytes_};
    const VkBufferCopy indices{staged.indices.offset, index_offset_, index_bytes_};
    vkCmdCopyBuffer(commands, staged.buffer, buffer_.get(), 1, &vertices);
    vkCmdCopyBuffer(commands, staged.buffer, buffer_.get(), 1, &indices);
}

BatchGeometry StaticMesh::geometry() const noexcept {
    return BatchGeometry{buffer_.get(), {0, vertex_bytes_}, {index_offset_, index_bytes_}, index_count_};
}

}
