#include "support/pixel_colors.hpp"

#include <cstdlib>

namespace minemod::renderer::test_support {
namespace {

constexpr int channel_tolerance = 2;

bool is_close(int drawn, int expected) {
    return std::abs(drawn - expected) <= channel_tolerance;
}

}

bool is_close(const Pixel& drawn, const Rgb& expected) {
    return is_close(drawn.red, expected.red) && is_close(drawn.green, expected.green)
        && is_close(drawn.blue, expected.blue);
}

std::string describe(const Pixel& pixel) {
    return "drawn " + std::to_string(pixel.red) + ", " + std::to_string(pixel.green) + ", "
        + std::to_string(pixel.blue);
}

}
