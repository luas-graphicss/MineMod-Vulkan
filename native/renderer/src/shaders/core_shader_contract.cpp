#include "shaders/core_shader_contract.hpp"

#include <algorithm>
#include <utility>

namespace minemod::renderer {

namespace {

template <typename Entry>
bool contains(const std::vector<Entry>& entries, const Entry& entry) {
    return std::ranges::find(entries, entry) != entries.end();
}

std::string stage_name(ShaderStage stage) {
    return stage == ShaderStage::vertex ? "vertex" : "fragment";
}

class Violations {
public:
    explicit Violations(const CoreShaderContract& contract) : contract_(contract) {}

    void check_stage(const ShaderInterface& shader, ShaderStage expected) {
        if (shader.stage != expected) {
            report("the " + stage_name(expected) + " stage was given a " + stage_name(shader.stage) + " shader");
        }
    }

    void check_resources(const ShaderInterface& shader) {
        for (const ResourceBinding& resource : shader.resources) {
            if (!contains(contract_.resources, resource)) {
                report(
                    "the " + stage_name(shader.stage) + " shader uses set " + std::to_string(resource.set) + " binding "
                    + std::to_string(resource.binding) + ", which is not bound");
            }
        }
    }

    void check_vertex_inputs(const ShaderInterface& vertex) {
        for (const std::uint32_t location : vertex.input_locations) {
            if (!contains(contract_.vertex_locations, location)) {
                report(
                    "the vertex shader reads location " + std::to_string(location)
                    + ", which the vertex format does not provide");
            }
        }
    }

    void check_varyings(const ShaderInterface& vertex, const ShaderInterface& fragment) {
        for (const std::uint32_t location : fragment.input_locations) {
            if (!contains(vertex.output_locations, location)) {
                report(
                    "the fragment shader reads location " + std::to_string(location)
                    + ", which the vertex shader does not write");
            }
        }
    }

    void check_color_outputs(const ShaderInterface& fragment) {
        for (const std::uint32_t output : contract_.color_outputs) {
            if (!contains(fragment.output_locations, output)) {
                report("the fragment shader does not write color output " + std::to_string(output));
            }
        }
        for (const std::uint32_t output : fragment.output_locations) {
            if (!contains(contract_.color_outputs, output)) {
                report(
                    "the fragment shader writes color output " + std::to_string(output) + ", which has no attachment");
            }
        }
    }

    std::vector<std::string> release() {
        return std::move(messages_);
    }

private:
    void report(const std::string& violation) {
        messages_.push_back("core shader '" + contract_.name + "': " + violation);
    }

    const CoreShaderContract& contract_;
    std::vector<std::string> messages_;
};

}

std::vector<std::string> contract_violations(
    const CoreShaderContract& contract, const ShaderInterface& vertex, const ShaderInterface& fragment) {
    Violations violations(contract);
    violations.check_stage(vertex, ShaderStage::vertex);
    violations.check_stage(fragment, ShaderStage::fragment);
    violations.check_vertex_inputs(vertex);
    violations.check_resources(vertex);
    violations.check_resources(fragment);
    violations.check_varyings(vertex, fragment);
    violations.check_color_outputs(fragment);
    return violations.release();
}

}
