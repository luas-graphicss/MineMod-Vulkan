#include "visibility/frustum.hpp"

#include <algorithm>

namespace minemod::renderer {

Frustum::Frustum(const Matrix4& view_projection) noexcept {
    const Plane horizontal = row(view_projection, 0);
    const Plane vertical = row(view_projection, 1);
    const Plane depth = row(view_projection, 2);
    const Plane clip = row(view_projection, 3);

    planes_ = {
        sum(clip, horizontal),
        difference(clip, horizontal),
        sum(clip, vertical),
        difference(clip, vertical),
        depth,
        difference(clip, depth),
    };
}

bool Frustum::intersects(const Box& box) const noexcept {
    return std::ranges::none_of(planes_, [&box](const Plane& plane) { return is_behind(plane, box); });
}

Frustum::Plane Frustum::row(const Matrix4& matrix, std::size_t index) noexcept {
    return Plane{matrix.at(index, 0), matrix.at(index, 1), matrix.at(index, 2), matrix.at(index, 3)};
}

Frustum::Plane Frustum::sum(const Plane& left, const Plane& right) noexcept {
    return Plane{left.x + right.x, left.y + right.y, left.z + right.z, left.offset + right.offset};
}

Frustum::Plane Frustum::difference(const Plane& left, const Plane& right) noexcept {
    return Plane{left.x - right.x, left.y - right.y, left.z - right.z, left.offset - right.offset};
}

bool Frustum::is_behind(const Plane& plane, const Box& box) noexcept {
    const float nearest_x = plane.x >= 0.0f ? box.maximum.x : box.minimum.x;
    const float nearest_y = plane.y >= 0.0f ? box.maximum.y : box.minimum.y;
    const float nearest_z = plane.z >= 0.0f ? box.maximum.z : box.minimum.z;
    return plane.x * nearest_x + plane.y * nearest_y + plane.z * nearest_z + plane.offset < 0.0f;
}

}
