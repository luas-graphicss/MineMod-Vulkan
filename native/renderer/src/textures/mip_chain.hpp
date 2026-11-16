#pragma once

#include <cstddef>
#include <cstdint>
#include <span>
#include <vector>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

struct MipLevel {
    VkExtent2D extent;
    std::size_t offset;
};

struct MipChain {
    std::vector<MipLevel> levels;
    std::vector<std::byte> rgba;

    std::span<const std::byte> level_bytes(std::size_t level) const;
};

std::uint32_t full_mip_level_count(VkExtent2D extent);

MipChain build_mip_chain(VkExtent2D extent, std::span<const std::byte> rgba, std::uint32_t level_count);

}
