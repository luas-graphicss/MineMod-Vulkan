#include <algorithm>
#include <cstdint>
#include <stdexcept>

#include <doctest.h>

#include "draw/element_defaults.hpp"
#include "draw/vertex_format.hpp"

namespace {

using namespace minemod::renderer;

constexpr std::uint32_t position_location = 0;
constexpr std::uint32_t color_location = 1;
constexpr std::uint32_t texture_location = 2;
constexpr std::uint32_t overlay_location = 3;
constexpr std::uint32_t light_location = 4;
constexpr std::uint32_t normal_location = 5;

constexpr VertexFormat particle_format{
    VertexElement::position, VertexElement::texture, VertexElement::color, VertexElement::light};
constexpr VertexFormat entity_format{
    VertexElement::position,
    VertexElement::color,
    VertexElement::texture,
    VertexElement::overlay,
    VertexElement::light,
    VertexElement::normal,
};

const VertexAttribute& attribute_at(const VertexLayout& layout, std::uint32_t location) {
    const auto found = std::ranges::find(layout.attributes, location, &VertexAttribute::location);
    REQUIRE(found != layout.attributes.end());
    return *found;
}

}

TEST_CASE("a vertex format packs its elements in the order they are listed") {
    const VertexLayout layout = particle_format.layout();

    CHECK(particle_format.stride() == 28);
    CHECK(layout.stride == 28);
    CHECK(attribute_at(layout, position_location) == VertexAttribute{position_location, VK_FORMAT_R32G32B32_SFLOAT, 0});
    CHECK(attribute_at(layout, texture_location) == VertexAttribute{texture_location, VK_FORMAT_R32G32_SFLOAT, 12});
    CHECK(attribute_at(layout, color_location) == VertexAttribute{color_location, VK_FORMAT_R8G8B8A8_UNORM, 20});
    CHECK(attribute_at(layout, light_location) == VertexAttribute{light_location, VK_FORMAT_R16G16_SINT, 24});
}

TEST_CASE("elements a format leaves out are read from the shared constants") {
    const VertexLayout layout = particle_format.layout();

    CHECK(layout.constant_stride == sizeof(ElementDefaults));
    CHECK(
        attribute_at(layout, overlay_location)
        == VertexAttribute{overlay_location, VK_FORMAT_R16G16_SINT, 12, VertexSource::shared_constant});
    CHECK(
        attribute_at(layout, normal_location)
        == VertexAttribute{normal_location, VK_FORMAT_R8G8B8A8_SNORM, 20, VertexSource::shared_constant});
    CHECK(layout.attributes.size() == vertex_element_count);
}

TEST_CASE("a format that carries every element reads nothing from the shared constants") {
    const VertexLayout layout = entity_format.layout();

    CHECK(entity_format.stride() == 36);
    CHECK(layout.constant_stride == 0);
    CHECK(std::ranges::none_of(layout.attributes, [](const VertexAttribute& attribute) {
        return attribute.source == VertexSource::shared_constant;
    }));
}

TEST_CASE("a vertex format needs a position and lists each element once") {
    CHECK_THROWS_AS(VertexFormat({VertexElement::color, VertexElement::texture}), std::invalid_argument);
    CHECK_THROWS_AS(
        VertexFormat({VertexElement::position, VertexElement::color, VertexElement::color}), std::invalid_argument);
}
