#pragma once

#include <compare>
#include <cstdint>
#include <vector>

#include "shaders/spirv_module.hpp"

namespace minemod::renderer {

enum class ShaderStage { vertex, fragment };

struct ResourceBinding {
    std::uint32_t set;
    std::uint32_t binding;

    friend auto operator<=>(const ResourceBinding&, const ResourceBinding&) = default;
};

struct ShaderInterface {
    ShaderStage stage;
    std::vector<std::uint32_t> input_locations;
    std::vector<std::uint32_t> output_locations;
    std::vector<ResourceBinding> resources;
};

ShaderInterface reflect_interface(const SpirvModule& module);

}
