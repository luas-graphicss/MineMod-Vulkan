#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

#include <vulkan/vulkan.h>

#include "pipelines/blending.hpp"

namespace minemod::renderer {

enum class VertexSource : std::uint8_t { each_vertex, shared_constant };

inline constexpr std::uint32_t per_vertex_binding = 0;
inline constexpr std::uint32_t shared_constant_binding = 1;

struct VertexAttribute {
    std::uint32_t location;
    VkFormat format;
    std::uint32_t offset;
    VertexSource source = VertexSource::each_vertex;

    friend bool operator==(const VertexAttribute&, const VertexAttribute&) = default;
};

struct VertexLayout {
    std::uint32_t stride;
    std::vector<VertexAttribute> attributes;
    std::uint32_t constant_stride = 0;

    friend bool operator==(const VertexLayout&, const VertexLayout&) = default;
};

struct DepthState {
    bool test;
    bool write;
    VkCompareOp compare;

    friend bool operator==(const DepthState&, const DepthState&) = default;
};

struct GraphicsPipelineState {
    VkShaderModule vertex_shader;
    VkShaderModule fragment_shader;
    VkPipelineLayout layout;
    VertexLayout vertex_layout;
    VkPrimitiveTopology topology;
    VkCullModeFlags cull_mode;
    DepthState depth;
    Blending blending;
    VkFormat color_format;
    VkFormat depth_format;

    friend bool operator==(const GraphicsPipelineState&, const GraphicsPipelineState&) = default;
};

struct GraphicsPipelineStateHash {
    std::size_t operator()(const GraphicsPipelineState& state) const noexcept;
};

}
