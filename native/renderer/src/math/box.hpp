#pragma once

#include "math/vector.hpp"

namespace minemod::renderer {

struct Box {
    Vector3 minimum;
    Vector3 maximum;
};

}
