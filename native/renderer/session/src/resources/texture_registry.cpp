#include "resources/texture_registry.hpp"

#include <algorithm>
#include <utility>

#include "minemod/abi/scene_commands.h"
#include "textures/mip_chain.hpp"

namespace minemod::scene {

namespace {

bool has_valid_shape(const TextureDefinition& definition) {
    const VkExtent2D extent = definition.extent;
    const bool has_valid_extent = extent.width != 0 && extent.height != 0
        && extent.width <= MINEMOD_TEXTURE_MAX_EXTENT && extent.height <= MINEMOD_TEXTURE_MAX_EXTENT;
    return has_valid_extent && definition.mip_levels != 0
        && definition.mip_levels <= renderer::full_mip_level_count(extent);
}

std::uint64_t chain_bytes(VkExtent2D extent, std::uint32_t mip_levels) noexcept {
    std::uint64_t bytes = 0;
    for (std::uint32_t level = 0; level < mip_levels; ++level) {
        const std::uint64_t width = std::max(extent.width >> level, 1u);
        const std::uint64_t height = std::max(extent.height >> level, 1u);
        bytes += width * height * MINEMOD_TEXEL_BYTES;
    }
    return bytes;
}

renderer::SamplerDescription sampler_for(const TextureDefinition& definition) noexcept {
    const bool is_linear = (definition.sampling & MINEMOD_SAMPLING_LINEAR) != 0;
    const bool blends_levels = (definition.sampling & MINEMOD_SAMPLING_BLEND_LEVELS) != 0;
    const bool repeats = (definition.sampling & MINEMOD_SAMPLING_REPEAT) != 0;
    const VkFilter filter = is_linear ? VK_FILTER_LINEAR : VK_FILTER_NEAREST;
    return renderer::SamplerDescription{
        filter,
        filter,
        blends_levels ? VK_SAMPLER_MIPMAP_MODE_LINEAR : VK_SAMPLER_MIPMAP_MODE_NEAREST,
        repeats ? VK_SAMPLER_ADDRESS_MODE_REPEAT : VK_SAMPLER_ADDRESS_MODE_CLAMP_TO_EDGE,
        definition.mip_levels,
    };
}

}

minemod_result TextureRegistry::define(const TextureDefinition& definition) {
    if (minemod_handle_kind(definition.texture) != MINEMOD_HANDLE_KIND_TEXTURE
        || textures_.contains(definition.texture)) {
        return MINEMOD_RESULT_INVALID_HANDLE;
    }
    if (!has_valid_shape(definition)) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    }
    if (textures_.size() >= max_textures_) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    const std::uint64_t bytes = chain_bytes(definition.extent, definition.mip_levels);
    textures_.emplace(
        definition.texture,
        SceneTexture{
            renderer::Texture(device_, definition.extent, definition.mip_levels),
            samplers_.sampler_for(sampler_for(definition)),
            definition.mip_levels,
            bytes,
        });
    texel_bytes_ += bytes;
    return MINEMOD_RESULT_OK;
}

SceneTexture* TextureRegistry::find(minemod_handle texture) {
    const auto found = textures_.find(texture);
    return found == textures_.end() ? nullptr : &found->second;
}

std::optional<SceneTexture> TextureRegistry::remove(minemod_handle texture) {
    const auto found = textures_.find(texture);
    if (found == textures_.end()) {
        return std::nullopt;
    }
    std::optional<SceneTexture> removed(std::move(found->second));
    texel_bytes_ -= removed->texel_bytes;
    textures_.erase(found);
    return removed;
}

}
