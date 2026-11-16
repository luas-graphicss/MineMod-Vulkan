#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <span>
#include <vector>

#include <vulkan/vulkan.h>

#include "draw/primitive_mode.hpp"
#include "draw/vertex_format.hpp"
#include "upload/arena_layout.hpp"
#include "upload/upload_arena.hpp"

namespace minemod::renderer {

inline constexpr VkBufferUsageFlags geometry_arena_usage =
    VK_BUFFER_USAGE_VERTEX_BUFFER_BIT | VK_BUFFER_USAGE_INDEX_BUFFER_BIT;

inline constexpr std::size_t max_batch_vertices = std::size_t{1} << 20;

struct BatchVertices {
    VertexFormat format;
    PrimitiveMode mode;
    std::span<const std::byte> bytes;
};

struct BatchGeometry {
    VkBuffer buffer;
    ArenaRegion vertices;
    ArenaRegion indices;
    std::uint32_t index_count;
};

std::uint32_t vertex_count(const BatchVertices& vertices);

class BatchStager {
public:
    explicit BatchStager(UploadArena& geometry_arena) noexcept : arena_(geometry_arena) {}

    std::optional<BatchGeometry> stage(const BatchVertices& vertices);

private:
    UploadArena& arena_;
    std::vector<std::uint32_t> indices_;
};

}
