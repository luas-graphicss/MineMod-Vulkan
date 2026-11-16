#pragma once

#include <cstdint>
#include <string>
#include <vector>

#include "shaders/shader_interface.hpp"

namespace minemod::renderer {

struct CoreShaderContract {
    std::string name;
    std::vector<std::uint32_t> vertex_locations;
    std::vector<ResourceBinding> resources;
    std::vector<std::uint32_t> color_outputs;
};

std::vector<std::string> contract_violations(
    const CoreShaderContract& contract, const ShaderInterface& vertex, const ShaderInterface& fragment);

}
