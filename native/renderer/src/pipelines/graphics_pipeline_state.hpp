#pragma once

#include <cstddef>
#include <cstdint>
#include <vector>

#include <vulkan/vulkan.h>

namespace minemod::renderer {

struct VertexAttribute {
    std::uint32_t location;
    VkFormat format;
    std::uint32_t offset;

    friend bool operator==(const VertexAttribute&, const VertexAttribute&) = default;
};

struct VertexLayout {
    std::uint32_t stride;
    std::vector<VertexAttribute> attributes;

    friend bool operator==(const VertexLayout&, const VertexLayout&) = default;
};

struct DepthState {
    bool test;
    bool write;
    VkCompareOp compare;

    friend bool operator==(const DepthState&, const DepthState&) = default;
};

enum class Blending { disabled, alpha };

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
