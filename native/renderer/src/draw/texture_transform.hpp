#pragma once

#include <array>

#include "math/matrix.hpp"

namespace minemod::renderer {

struct TextureTransform {
    std::array<float, 3> u_row{1.0f, 0.0f, 0.0f};
    std::array<float, 3> v_row{0.0f, 1.0f, 0.0f};

    friend bool operator==(const TextureTransform&, const TextureTransform&) = default;
};

static_assert(sizeof(TextureTransform) == 24, "texture transforms are pushed to the shaders without padding");

inline TextureTransform texture_transform(const Matrix4& matrix) noexcept {
    return TextureTransform{
        {matrix.at(0, 0), matrix.at(0, 1), matrix.at(0, 3)},
        {matrix.at(1, 0), matrix.at(1, 1), matrix.at(1, 3)},
    };
}

}
