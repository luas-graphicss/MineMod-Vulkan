#include "shaders/shader_interface.hpp"

#include <algorithm>
#include <cstddef>
#include <optional>
#include <span>

namespace minemod::renderer {

namespace {

constexpr std::uint16_t op_entry_point = 15;
constexpr std::uint16_t op_variable = 59;
constexpr std::uint16_t op_decorate = 71;

constexpr std::uint32_t execution_model_vertex = 0;
constexpr std::uint32_t execution_model_fragment = 4;

constexpr std::uint32_t decoration_location = 30;
constexpr std::uint32_t decoration_binding = 33;
constexpr std::uint32_t decoration_descriptor_set = 34;

constexpr std::uint32_t storage_uniform_constant = 0;
constexpr std::uint32_t storage_input = 1;
constexpr std::uint32_t storage_uniform = 2;
constexpr std::uint32_t storage_output = 3;
constexpr std::uint32_t storage_buffer = 12;

struct Decorations {
    std::optional<std::uint32_t> location;
    std::optional<std::uint32_t> binding;
    std::optional<std::uint32_t> descriptor_set;
};

struct Variable {
    std::uint32_t id;
    std::uint32_t storage;
};

std::span<const std::uint32_t> fixed_operands(const SpirvInstruction& instruction, std::size_t count) {
    if (instruction.operands.size() < count) {
        throw ShaderError("a SPIR-V instruction is shorter than its fixed operands");
    }
    return instruction.operands.first(count);
}

std::uint32_t known_id(const SpirvModule& module, std::uint32_t id) {
    if (id >= module.id_bound()) {
        throw ShaderError("a SPIR-V instruction names an id beyond the module id bound");
    }
    return id;
}

ShaderStage stage_of(std::uint32_t execution_model) {
    if (execution_model == execution_model_vertex) {
        return ShaderStage::vertex;
    }
    if (execution_model == execution_model_fragment) {
        return ShaderStage::fragment;
    }
    throw ShaderError("the shader is neither a vertex nor a fragment shader");
}

void record_decoration(
    const SpirvModule& module, const SpirvInstruction& instruction, std::span<Decorations> decorations) {
    const std::span<const std::uint32_t> target_and_kind = fixed_operands(instruction, 2);
    Decorations& target = decorations[known_id(module, target_and_kind[0])];
    const std::uint32_t kind = target_and_kind[1];
    if (kind == decoration_location) {
        target.location = fixed_operands(instruction, 3)[2];
    } else if (kind == decoration_binding) {
        target.binding = fixed_operands(instruction, 3)[2];
    } else if (kind == decoration_descriptor_set) {
        target.descriptor_set = fixed_operands(instruction, 3)[2];
    }
}

bool is_resource(std::uint32_t storage) {
    return storage == storage_uniform_constant || storage == storage_uniform || storage == storage_buffer;
}

void record_variable(const Variable& variable, const Decorations& decorations, ShaderInterface& interface) {
    if (variable.storage == storage_input && decorations.location) {
        interface.input_locations.push_back(*decorations.location);
    } else if (variable.storage == storage_output && decorations.location) {
        interface.output_locations.push_back(*decorations.location);
    } else if (is_resource(variable.storage)) {
        if (!decorations.descriptor_set || !decorations.binding) {
            throw ShaderError("a shader resource has no descriptor set and binding");
        }
        interface.resources.push_back(ResourceBinding{*decorations.descriptor_set, *decorations.binding});
    }
}

template <typename Entry>
void sort_unique(std::vector<Entry>& entries) {
    std::ranges::sort(entries);
    const auto duplicates = std::ranges::unique(entries);
    entries.erase(duplicates.begin(), duplicates.end());
}

}

ShaderInterface reflect_interface(const SpirvModule& module) {
    std::vector<Decorations> decorations(module.id_bound());
    std::vector<Variable> variables;
    std::optional<ShaderStage> stage;
    for (const SpirvInstruction& instruction : module.instructions()) {
        if (instruction.opcode == op_decorate) {
            record_decoration(module, instruction, decorations);
        } else if (instruction.opcode == op_variable) {
            const std::span<const std::uint32_t> operands = fixed_operands(instruction, 3);
            variables.push_back(Variable{known_id(module, operands[1]), operands[2]});
        } else if (instruction.opcode == op_entry_point) {
            if (stage) {
                throw ShaderError("the shader module has more than one entry point");
            }
            stage = stage_of(fixed_operands(instruction, 1)[0]);
        }
    }
    if (!stage) {
        throw ShaderError("the shader module has no entry point");
    }

    ShaderInterface interface{*stage, {}, {}, {}};
    for (const Variable& variable : variables) {
        record_variable(variable, decorations[variable.id], interface);
    }
    sort_unique(interface.input_locations);
    sort_unique(interface.output_locations);
    sort_unique(interface.resources);
    return interface;
}

}
