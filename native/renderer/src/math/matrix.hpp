#pragma once

#include <array>
#include <cstddef>

namespace minemod::renderer {

struct Matrix4 {
    std::array<float, 16> columns;

    float at(std::size_t row, std::size_t column) const noexcept {
        return columns[column * 4 + row];
    }
};

inline constexpr Matrix4 identity_matrix{{
    1.0f, 0.0f, 0.0f, 0.0f,
    0.0f, 1.0f, 0.0f, 0.0f,
    0.0f, 0.0f, 1.0f, 0.0f,
    0.0f, 0.0f, 0.0f, 1.0f,
}};

struct PerspectiveLens {
    float vertical_field_of_view;
    float aspect_ratio;
    float near_distance;
    float far_distance;
};

Matrix4 multiply(const Matrix4& left, const Matrix4& right) noexcept;
Matrix4 perspective(const PerspectiveLens& lens);
Matrix4 rotation_about_y(float radians) noexcept;

}
