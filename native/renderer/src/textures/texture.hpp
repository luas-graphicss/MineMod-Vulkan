#pragma once

#include <cstdint>

#include <vulkan/vulkan.h>

#include "device/device.hpp"
#include "resources/image.hpp"
#include "textures/atlas_layout.hpp"
#include "textures/mip_chain.hpp"
#include "upload/arena_layout.hpp"

namespace minemod::renderer {

inline constexpr VkFormat texture_format = VK_FORMAT_R8G8B8A8_UNORM;

class Texture {
public:
    Texture(const Device& device, VkExtent2D extent, std::uint32_t mip_levels);

    const Image& image() const noexcept {
        return image_;
    }

    void record_upload(
        VkCommandBuffer commands, VkBuffer staging, const ArenaRegion& staged, const MipChain& chain) const;
    void record_region_update(
        VkCommandBuffer commands,
        VkBuffer staging,
        const ArenaRegion& staged,
        std::uint32_t level,
        const SpriteRegion& region) const;

private:
    Image image_;
    std::uint32_t mip_levels_;
};

}
