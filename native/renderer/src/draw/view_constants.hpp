#pragma once

#include <array>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "environment/fog.hpp"
#include "math/matrix.hpp"
#include "math/vector.hpp"
#include "resources/buffer.hpp"

namespace minemod::renderer {

struct SceneView {
    Matrix4 projection;
    Fog fog;
    std::array<Vector3, 2> light_directions;
};

inline constexpr VkDeviceSize view_constants_bytes = 128;

class ViewConstants {
public:
    explicit ViewConstants(const Device& device);

    void write(const SceneView& view) const;

    VkBuffer buffer() const noexcept {
        return constants_.get();
    }

private:
    Buffer constants_;
};

}
