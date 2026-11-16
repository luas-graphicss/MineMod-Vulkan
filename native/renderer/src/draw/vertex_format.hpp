#pragma once

#include <array>
#include <cstddef>
#include <cstdint>
#include <initializer_list>
#include <span>
#include <stdexcept>

#include "pipelines/graphics_pipeline_state.hpp"

namespace minemod::renderer {

enum class VertexElement : std::uint8_t { position, color, texture, overlay, light, normal };

inline constexpr std::size_t vertex_element_count = 6;

constexpr std::uint32_t element_bytes(VertexElement element) noexcept {
    switch (element) {
    case VertexElement::position:
        return 12;
    case VertexElement::texture:
        return 8;
    case VertexElement::color:
    case VertexElement::overlay:
    case VertexElement::light:
    case VertexElement::normal:
        break;
    }
    return 4;
}

class VertexFormat {
public:
    constexpr VertexFormat(std::initializer_list<VertexElement> elements)
        : VertexFormat(std::span<const VertexElement>(elements.begin(), elements.size())) {}

    constexpr explicit VertexFormat(std::span<const VertexElement> elements) {
        for (const VertexElement element : elements) {
            if (has(element)) {
                throw std::invalid_argument("a vertex format lists each element once");
            }
            elements_[count_] = element;
            ++count_;
            stride_ += element_bytes(element);
        }
        if (!has(VertexElement::position)) {
            throw std::invalid_argument("a vertex format needs a position");
        }
    }

    constexpr bool has(VertexElement element) const noexcept {
        for (std::size_t index = 0; index < count_; ++index) {
            if (elements_[index] == element) {
                return true;
            }
        }
        return false;
    }

    constexpr std::span<const VertexElement> elements() const noexcept {
        return std::span(elements_).first(count_);
    }

    constexpr std::uint32_t stride() const noexcept {
        return stride_;
    }

    VertexLayout layout() const;

    friend constexpr bool operator==(const VertexFormat&, const VertexFormat&) = default;

private:
    std::array<VertexElement, vertex_element_count> elements_{};
    std::size_t count_ = 0;
    std::uint32_t stride_ = 0;
};

}
