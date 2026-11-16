#pragma once

namespace minemod::renderer {

struct Vector3 {
    float x;
    float y;
    float z;

    friend bool operator==(const Vector3&, const Vector3&) = default;
};

struct WorldPosition {
    double x;
    double y;
    double z;
};

}
