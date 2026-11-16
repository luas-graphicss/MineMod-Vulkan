#include "draw/quad_order.hpp"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstring>
#include <limits>
#include <numeric>
#include <stdexcept>
#include <vector>

namespace minemod::renderer {
namespace {

constexpr std::size_t corners_per_quad = 4;
constexpr std::size_t triangle_indices_per_quad = 6;
constexpr std::size_t opposite_corner = 2;

std::size_t position_offset(const VertexFormat& format) noexcept {
    std::size_t offset = 0;
    for (const VertexElement element : format.elements()) {
        if (element == VertexElement::position) {
            break;
        }
        offset += element_bytes(element);
    }
    return offset;
}

std::array<float, 3> position_at(std::span<const std::byte> vertices, std::size_t offset) {
    std::array<float, 3> position{};
    std::memcpy(position.data(), vertices.data() + offset, sizeof(position));
    return position;
}

float precedence(QuadOrder order, const std::array<float, 3>& corner, const std::array<float, 3>& opposite) noexcept {
    const float x = (corner[0] + opposite[0]) / 2.0f;
    const float y = (corner[1] + opposite[1]) / 2.0f;
    const float z = (corner[2] + opposite[2]) / 2.0f;
    const float drawn_earlier_when_larger = order == QuadOrder::rising_depth ? -z : x * x + y * y + z * z;
    return std::isnan(drawn_earlier_when_larger) ? std::numeric_limits<float>::infinity() : drawn_earlier_when_larger;
}

std::vector<float> quad_precedences(
    const VertexFormat& format, std::span<const std::byte> vertices, QuadOrder order, std::size_t quad_count) {
    const std::size_t stride = format.stride();
    const std::size_t offset = position_offset(format);
    std::vector<float> precedences;
    precedences.reserve(quad_count);
    for (std::size_t quad = 0; quad < quad_count; ++quad) {
        const std::size_t first_corner = quad * corners_per_quad * stride + offset;
        precedences.push_back(precedence(
            order, position_at(vertices, first_corner), position_at(vertices, first_corner + opposite_corner * stride)));
    }
    return precedences;
}

}

void order_quads(
    const VertexFormat& format, std::span<const std::byte> vertices, QuadOrder order, std::span<std::uint32_t> triangles) {
    const std::size_t quad_count = triangles.size() / triangle_indices_per_quad;
    const std::size_t quad_bytes = corners_per_quad * format.stride();
    if (triangles.size() % triangle_indices_per_quad != 0 || vertices.size() != quad_count * quad_bytes) {
        throw std::invalid_argument("the indices do not triangulate the quads of the vertices");
    }
    if (order == QuadOrder::submitted) {
        return;
    }

    const std::vector<float> precedences = quad_precedences(format, vertices, order, quad_count);
    std::vector<std::size_t> drawing_order(quad_count);
    std::iota(drawing_order.begin(), drawing_order.end(), std::size_t{0});
    std::ranges::stable_sort(drawing_order, [&precedences](std::size_t left, std::size_t right) {
        return precedences[left] > precedences[right];
    });

    const std::vector<std::uint32_t> submitted(triangles.begin(), triangles.end());
    for (std::size_t drawn = 0; drawn < quad_count; ++drawn) {
        const std::span<const std::uint32_t> quad = std::span(submitted).subspan(
            drawing_order[drawn] * triangle_indices_per_quad, triangle_indices_per_quad);
        std::ranges::copy(quad, triangles.subspan(drawn * triangle_indices_per_quad).begin());
    }
}

}
