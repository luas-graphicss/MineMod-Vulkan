#pragma once

#include <cstddef>
#include <cstdint>
#include <span>

#include "draw/vertex_format.hpp"

namespace minemod::renderer {

enum class QuadOrder : std::uint8_t { submitted, farthest_first, rising_depth };

void order_quads(
    const VertexFormat& format, std::span<const std::byte> vertices, QuadOrder order, std::span<std::uint32_t> triangles);

}
