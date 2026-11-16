#pragma once

#include <cstdint>
#include <optional>
#include <span>
#include <string>
#include <vector>

#include "shaders/core_shader_contract.hpp"
#include "shaders/spirv_module.hpp"

namespace minemod::renderer {

struct CheckedProgram {
    SpirvModule vertex;
    SpirvModule fragment;
};

struct ProgramCheck {
    std::optional<CheckedProgram> program;
    std::vector<std::string> violations;
};

ProgramCheck check_program(
    const CoreShaderContract& contract,
    std::span<const std::uint32_t> vertex,
    std::span<const std::uint32_t> fragment,
    const SpirvLimits& limits = default_spirv_limits);

}
