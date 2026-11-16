#pragma once

#include <string>

#include "frame/frame_readback.hpp"

namespace minemod::renderer::test_support {

struct Rgb {
    int red;
    int green;
    int blue;
};

bool is_close(const Pixel& drawn, const Rgb& expected);

std::string describe(const Pixel& pixel);

}
