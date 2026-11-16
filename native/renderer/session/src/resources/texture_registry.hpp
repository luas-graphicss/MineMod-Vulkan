#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <unordered_map>

#include <vulkan/vulkan.h>

#include "commands/scene_commands.hpp"
#include "device/device.hpp"
#include "minemod/abi/handle.h"
#include "minemod/abi/result.h"
#include "textures/sampler_cache.hpp"
#include "textures/texture.hpp"

namespace minemod::scene {

struct SceneTexture {
    renderer::Texture texture;
    VkSampler sampler;
    std::uint32_t mip_levels;
    std::uint64_t texel_bytes;
    bool is_readable = false;
};

class TextureRegistry {
public:
    TextureRegistry(const renderer::Device& device, renderer::SamplerCache& samplers, std::size_t max_textures) noexcept
        : device_(device), samplers_(samplers), max_textures_(max_textures) {}

    minemod_result define(const TextureDefinition& definition);
    SceneTexture* find(minemod_handle texture);
    std::optional<SceneTexture> remove(minemod_handle texture);

    std::size_t size() const noexcept {
        return textures_.size();
    }

    std::uint64_t texel_bytes() const noexcept {
        return texel_bytes_;
    }

private:
    const renderer::Device& device_;
    renderer::SamplerCache& samplers_;
    std::size_t max_textures_;
    std::unordered_map<minemod_handle, SceneTexture> textures_;
    std::uint64_t texel_bytes_ = 0;
};

}
