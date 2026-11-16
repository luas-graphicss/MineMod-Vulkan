#pragma once

#include <array>
#include <cstddef>

#include "math/box.hpp"
#include "math/matrix.hpp"

namespace minemod::renderer {

class Frustum {
public:
    explicit Frustum(const Matrix4& view_projection) noexcept;

    bool intersects(const Box& box) const noexcept;

private:
    struct Plane {
        float x;
        float y;
        float z;
        float offset;
    };

    static Plane row(const Matrix4& matrix, std::size_t index) noexcept;
    static Plane sum(const Plane& left, const Plane& right) noexcept;
    static Plane difference(const Plane& left, const Plane& right) noexcept;
    static bool is_behind(const Plane& plane, const Box& box) noexcept;

    std::array<Plane, 6> planes_;
};

}
