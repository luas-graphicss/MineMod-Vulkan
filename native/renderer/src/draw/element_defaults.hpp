#pragma once

#include <array>
#include <cstdint>

namespace minemod::renderer {

struct ElementDefaults {
    std::array<std::uint8_t, 4> color{255, 255, 255, 255};
    std::array<float, 2> texture_coordinate{0.0f, 0.0f};
    std::array<std::int16_t, 2> overlay_texel{0, 10};
    std::array<std::int16_t, 2> packed_light{240, 240};
    std::array<std::int8_t, 4> normal{0, 127, 0, 0};
};

static_assert(sizeof(ElementDefaults) == 24, "element defaults are bound to the pipeline without padding");

}
