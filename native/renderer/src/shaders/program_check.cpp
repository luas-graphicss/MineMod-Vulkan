#include "shaders/program_check.hpp"

#include <utility>

#include "shaders/shader_interface.hpp"

namespace minemod::renderer {

ProgramCheck check_program(
    const CoreShaderContract& contract,
    std::span<const std::uint32_t> vertex,
    std::span<const std::uint32_t> fragment,
    const SpirvLimits& limits) {
    ProgramCheck check;
    try {
        SpirvModule vertex_module(vertex, limits);
        SpirvModule fragment_module(fragment, limits);
        check.violations =
            contract_violations(contract, reflect_interface(vertex_module), reflect_interface(fragment_module));
        if (check.violations.empty()) {
            check.program = CheckedProgram{std::move(vertex_module), std::move(fragment_module)};
        }
    } catch (const ShaderError& error) {
        check.violations.push_back("core shader '" + contract.name + "': " + error.what());
    }
    return check;
}

}
