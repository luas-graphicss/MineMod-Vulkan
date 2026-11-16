#include "math/matrix.hpp"

#include <cmath>
#include <numbers>
#include <stdexcept>

namespace minemod::renderer {
namespace {

float row_times_column(const Matrix4& left, const Matrix4& right, std::size_t row, std::size_t column) noexcept {
    float sum = 0.0f;
    for (std::size_t inner = 0; inner < 4; ++inner) {
        sum += left.at(row, inner) * right.at(inner, column);
    }
    return sum;
}

bool is_usable(const PerspectiveLens& lens) noexcept {
    return lens.vertical_field_of_view > 0.0f && lens.vertical_field_of_view < std::numbers::pi_v<float>
        && lens.aspect_ratio > 0.0f && lens.near_distance > 0.0f && lens.far_distance > lens.near_distance;
}

}

Matrix4 multiply(const Matrix4& left, const Matrix4& right) noexcept {
    Matrix4 product{};
    for (std::size_t cell = 0; cell < product.columns.size(); ++cell) {
        product.columns[cell] = row_times_column(left, right, cell % 4, cell / 4);
    }
    return product;
}

Matrix4 perspective(const PerspectiveLens& lens) {
    if (!is_usable(lens)) {
        throw std::invalid_argument(
            "a perspective lens needs a field of view inside (0, pi), a positive aspect ratio and 0 < near < far");
    }

    const float focal_length = 1.0f / std::tan(lens.vertical_field_of_view * 0.5f);
    const float depth_span = lens.near_distance - lens.far_distance;

    Matrix4 projection{};
    projection.columns[0] = focal_length / lens.aspect_ratio;
    projection.columns[5] = focal_length;
    projection.columns[10] = lens.far_distance / depth_span;
    projection.columns[11] = -1.0f;
    projection.columns[14] = lens.near_distance * lens.far_distance / depth_span;
    return projection;
}

Matrix4 rotation_about_y(float radians) noexcept {
    const float cosine = std::cos(radians);
    const float sine = std::sin(radians);

    Matrix4 rotation = identity_matrix;
    rotation.columns[0] = cosine;
    rotation.columns[2] = -sine;
    rotation.columns[8] = sine;
    rotation.columns[10] = cosine;
    return rotation;
}

}
