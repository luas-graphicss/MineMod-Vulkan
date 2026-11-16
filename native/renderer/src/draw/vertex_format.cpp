#include "draw/vertex_format.hpp"

#include <cstddef>

#include <vulkan/vulkan.h>

#include "draw/element_defaults.hpp"

namespace minemod::renderer {
namespace {

constexpr std::array defaulted_elements{
    VertexElement::color,
    VertexElement::texture,
    VertexElement::overlay,
    VertexElement::light,
    VertexElement::normal,
};

constexpr std::uint32_t location_of(VertexElement element) noexcept {
    return static_cast<std::uint32_t>(element);
}

constexpr VkFormat format_of(VertexElement element) noexcept {
    switch (element) {
    case VertexElement::position:
        return VK_FORMAT_R32G32B32_SFLOAT;
    case VertexElement::color:
        return VK_FORMAT_R8G8B8A8_UNORM;
    case VertexElement::texture:
        return VK_FORMAT_R32G32_SFLOAT;
    case VertexElement::overlay:
    case VertexElement::light:
        return VK_FORMAT_R16G16_SINT;
    case VertexElement::normal:
        break;
    }
    return VK_FORMAT_R8G8B8A8_SNORM;
}

constexpr std::uint32_t default_offset(VertexElement element) noexcept {
    switch (element) {
    case VertexElement::texture:
        return offsetof(ElementDefaults, texture_coordinate);
    case VertexElement::overlay:
        return offsetof(ElementDefaults, overlay_texel);
    case VertexElement::light:
        return offsetof(ElementDefaults, packed_light);
    case VertexElement::normal:
        return offsetof(ElementDefaults, normal);
    case VertexElement::position:
    case VertexElement::color:
        break;
    }
    return offsetof(ElementDefaults, color);
}

}

VertexLayout VertexFormat::layout() const {
    VertexLayout layout{stride_, {}};
    layout.attributes.reserve(vertex_element_count);

    std::uint32_t offset = 0;
    for (const VertexElement element : elements()) {
        layout.attributes.push_back({location_of(element), format_of(element), offset});
        offset += element_bytes(element);
    }
    for (const VertexElement element : defaulted_elements) {
        if (!has(element)) {
            layout.attributes.push_back(
                {location_of(element), format_of(element), default_offset(element), VertexSource::shared_constant});
            layout.constant_stride = sizeof(ElementDefaults);
        }
    }
    return layout;
}

}
