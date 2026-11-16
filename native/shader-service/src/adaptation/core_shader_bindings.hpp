#pragma once

#include <cstdint>
#include <span>
#include <string_view>

#include "source/stage.hpp"

namespace minemod::shaders {

struct AttributeBinding {
    std::string_view name;
    std::string_view type;
    std::uint32_t location;
};

struct UniformBinding {
    std::string_view name;
    std::string_view type;
    std::string_view expression;
};

struct SamplerBinding {
    std::string_view name;
    Stage stage;
    std::uint32_t set;
    std::uint32_t binding;
};

struct CoreShaderBindings {
    std::string_view contract;
    std::uint32_t revision;
    std::string_view constant_blocks;
    std::span<const AttributeBinding> attributes;
    std::span<const UniformBinding> uniforms;
    std::span<const SamplerBinding> samplers;
};

const CoreShaderBindings& scene_bindings() noexcept;

}
