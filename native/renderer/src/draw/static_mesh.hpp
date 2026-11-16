#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "draw/batch_geometry.hpp"
#include "resources/buffer.hpp"

namespace minemod::renderer {

class StaticMesh {
public:
    StaticMesh(const Device& device, const BatchGeometry& staged);

    void record_upload(VkCommandBuffer commands, const BatchGeometry& staged) const;

    BatchGeometry geometry() const noexcept;

    VkDeviceSize size_bytes() const noexcept {
        return index_offset_ + index_bytes_;
    }

private:
    VkDeviceSize vertex_bytes_;
    VkDeviceSize index_bytes_;
    VkDeviceSize index_offset_;
    std::uint32_t index_count_;
    Buffer buffer_;
};

}
