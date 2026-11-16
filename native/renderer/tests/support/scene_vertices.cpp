#include "support/scene_vertices.hpp"

namespace minemod::renderer::test_support {
namespace {

template <typename Field>
void append(std::vector<std::byte>& bytes, const Field& field) {
    const std::span<const std::byte> encoded = std::as_bytes(std::span(&field, 1));
    bytes.insert(bytes.end(), encoded.begin(), encoded.end());
}

void append_element(std::vector<std::byte>& bytes, VertexElement element, const SceneVertex& vertex) {
    switch (element) {
    case VertexElement::position:
        append(bytes, vertex.position);
        return;
    case VertexElement::color:
        append(bytes, vertex.color);
        return;
    case VertexElement::texture:
        append(bytes, vertex.texture_coordinate);
        return;
    case VertexElement::overlay:
        append(bytes, vertex.overlay_texel);
        return;
    case VertexElement::light:
        append(bytes, vertex.packed_light);
        return;
    case VertexElement::normal:
        append(bytes, vertex.normal);
        return;
    }
}

}

std::vector<std::byte> encode_vertices(const VertexFormat& format, std::span<const SceneVertex> vertices) {
    std::vector<std::byte> bytes;
    bytes.reserve(vertices.size() * format.stride());
    for (const SceneVertex& vertex : vertices) {
        for (const VertexElement element : format.elements()) {
            append_element(bytes, element, vertex);
        }
    }
    return bytes;
}

std::array<SceneVertex, 4> quad_corners(const QuadPlacement& placement, const SceneVertex& surface) {
    std::array<SceneVertex, 4> counter_clockwise{surface, surface, surface, surface};
    counter_clockwise[0].position = {placement.left, placement.bottom, placement.depth};
    counter_clockwise[1].position = {placement.right, placement.bottom, placement.depth};
    counter_clockwise[2].position = {placement.right, placement.top, placement.depth};
    counter_clockwise[3].position = {placement.left, placement.top, placement.depth};
    return counter_clockwise;
}

}
