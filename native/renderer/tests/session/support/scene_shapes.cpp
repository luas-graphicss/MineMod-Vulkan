#include "session/support/scene_shapes.hpp"

#include <bit>
#include <cstddef>

#include "session/support/scene_stream.hpp"

namespace minemod::scene::test_support {

namespace {

struct Corner {
    float x;
    float y;
    float u;
    float v;
};

void append_float(std::vector<std::uint8_t>& bytes, float value) {
    const auto bits = std::bit_cast<std::uint32_t>(value);
    for (std::size_t index = 0; index < sizeof(bits); ++index) {
        bytes.push_back(static_cast<std::uint8_t>(bits >> (index * 8)));
    }
}

std::array<Corner, 4> corners_of(const VkRect2D& pixels) {
    const auto left = static_cast<float>(pixels.offset.x);
    const auto top = static_cast<float>(pixels.offset.y);
    const float right = left + static_cast<float>(pixels.extent.width);
    const float bottom = top + static_cast<float>(pixels.extent.height);
    return {Corner{left, top, 0.0f, 0.0f}, {left, bottom, 0.0f, 1.0f}, {right, bottom, 1.0f, 1.0f}, {right, top, 1.0f, 0.0f}};
}

void append_colored_corner(std::vector<std::uint8_t>& bytes, const Corner& corner, const Color& color) {
    append_float(bytes, corner.x);
    append_float(bytes, corner.y);
    append_float(bytes, interface_depth);
    bytes.insert(bytes.end(), color.begin(), color.end());
}

}

std::array<float, 16> pixel_projection(VkExtent2D extent) {
    std::array<float, 16> projection = identity;
    projection[0] = 2.0f / static_cast<float>(extent.width);
    projection[5] = -2.0f / static_cast<float>(extent.height);
    projection[12] = -1.0f;
    projection[13] = 1.0f;
    return projection;
}

std::vector<std::uint8_t> colored_rectangle(const VkRect2D& pixels, const Color& color) {
    std::vector<std::uint8_t> bytes;
    for (const Corner& corner : corners_of(pixels)) {
        append_colored_corner(bytes, corner, color);
    }
    return bytes;
}

std::vector<std::uint8_t> textured_rectangle(const VkRect2D& pixels, const Color& color) {
    std::vector<std::uint8_t> bytes;
    for (const Corner& corner : corners_of(pixels)) {
        append_colored_corner(bytes, corner, color);
        append_float(bytes, corner.u);
        append_float(bytes, corner.v);
    }
    return bytes;
}

std::vector<std::uint8_t> solid_texels(VkExtent2D extent, const Color& color) {
    std::vector<std::uint8_t> texels;
    for (std::uint32_t texel = 0; texel < extent.width * extent.height; ++texel) {
        texels.insert(texels.end(), color.begin(), color.end());
    }
    return texels;
}

}
