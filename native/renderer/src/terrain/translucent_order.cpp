#include "terrain/translucent_order.hpp"

#include <algorithm>
#include <array>
#include <cmath>
#include <cstddef>
#include <limits>
#include <numeric>
#include <span>
#include <stdexcept>

#include "terrain/terrain_layer.hpp"

namespace minemod::renderer {
namespace {

Vector3 center_of(std::span<const std::uint32_t> corners, std::span<const TerrainVertex> vertices) {
    Vector3 sum{0.0f, 0.0f, 0.0f};
    for (const std::uint32_t corner : corners) {
        const std::array<float, 3>& position = vertices[corner].position;
        sum = Vector3{sum.x + position[0], sum.y + position[1], sum.z + position[2]};
    }
    const auto corner_count = static_cast<float>(corners.size());
    return Vector3{sum.x / corner_count, sum.y / corner_count, sum.z / corner_count};
}

float comparable_squared_distance(const Vector3& from, const Vector3& to) noexcept {
    const float x = to.x - from.x;
    const float y = to.y - from.y;
    const float z = to.z - from.z;
    const float squared = x * x + y * y + z * z;
    return std::isnan(squared) ? std::numeric_limits<float>::infinity() : squared;
}

}

TranslucentQuads translucent_quads(const SectionMesh& mesh) {
    require_drawable(mesh);
    const IndexRange& layer = mesh.layers[layer_index(TerrainLayer::translucent)];
    if (layer.index_count % indices_per_quad != 0) {
        throw std::invalid_argument("the translucent layer of a section must hold whole quads");
    }

    const std::span<const std::uint32_t> indices =
        std::span(mesh.indices).subspan(layer.first_index, layer.index_count);
    TranslucentQuads quads;
    quads.indices.assign(indices.begin(), indices.end());
    quads.centers.reserve(indices.size() / indices_per_quad);
    for (std::size_t first = 0; first < indices.size(); first += indices_per_quad) {
        quads.centers.push_back(center_of(indices.subspan(first, indices_per_quad), mesh.vertices));
    }
    return quads;
}

std::vector<std::uint32_t> far_to_near(const TranslucentQuads& quads, const Vector3& camera_in_section) {
    if (quads.indices.size() != quads.centers.size() * indices_per_quad) {
        throw std::invalid_argument("translucent quads need six indices for each center");
    }

    std::vector<float> distances;
    distances.reserve(quads.centers.size());
    for (const Vector3& center : quads.centers) {
        distances.push_back(comparable_squared_distance(camera_in_section, center));
    }

    std::vector<std::size_t> drawing_order(quads.centers.size());
    std::iota(drawing_order.begin(), drawing_order.end(), std::size_t{0});
    std::ranges::stable_sort(drawing_order, [&distances](std::size_t left, std::size_t right) {
        return distances[left] > distances[right];
    });

    std::vector<std::uint32_t> ordered;
    ordered.reserve(quads.indices.size());
    for (const std::size_t quad : drawing_order) {
        const std::span<const std::uint32_t> corners =
            std::span(quads.indices).subspan(quad * indices_per_quad, indices_per_quad);
        ordered.insert(ordered.end(), corners.begin(), corners.end());
    }
    return ordered;
}

}
