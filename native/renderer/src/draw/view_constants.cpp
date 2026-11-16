#include "draw/view_constants.hpp"

#include <cstddef>
#include <cstdint>
#include <span>
#include <stdexcept>

namespace minemod::renderer {
namespace {

struct ViewBlock {
    std::array<float, 16> projection;
    std::array<float, 4> fog_color;
    std::array<std::array<float, 4>, 2> light_directions;
    float fog_start;
    float fog_end;
    std::uint32_t fog_shape;
    std::uint32_t block_alignment_tail;
};

static_assert(sizeof(ViewBlock) == view_constants_bytes, "the view block mirrors the std140 layout of the shaders");

std::array<float, 4> as_direction(const Vector3& direction) noexcept {
    return {direction.x, direction.y, direction.z, 0.0f};
}

}

ViewConstants::ViewConstants(const Device& device)
    : constants_(device, view_constants_bytes, VK_BUFFER_USAGE_UNIFORM_BUFFER_BIT, host_memory) {}

void ViewConstants::write(const SceneView& view) const {
    if (!has_depth(view.fog)) {
        throw std::invalid_argument("view fog must end farther from the camera than it starts");
    }
    const ViewBlock block{
        view.projection.columns,
        view.fog.color,
        {as_direction(view.light_directions[0]), as_direction(view.light_directions[1])},
        view.fog.start,
        view.fog.end,
        static_cast<std::uint32_t>(view.fog.shape),
        0,
    };
    constants_.write(std::as_bytes(std::span(&block, 1)));
}

}
