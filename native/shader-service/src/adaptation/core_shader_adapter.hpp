#pragma once

#include <optional>
#include <string>
#include <vector>

#include "adaptation/core_shader_bindings.hpp"
#include "source/include_expansion.hpp"

namespace minemod::shaders {

struct AdaptedProgram {
    std::string vertex;
    std::string fragment;
};

struct Adaptation {
    std::optional<AdaptedProgram> program;
    std::vector<std::string> problems;
};

Adaptation adapt_core_shader(
    const CoreShaderBindings& bindings, const ExpandedSource& vertex, const ExpandedSource& fragment);

}
