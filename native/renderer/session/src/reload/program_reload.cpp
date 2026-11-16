#include "reload/program_reload.hpp"

#include <iterator>
#include <utility>
#include <vector>

#include "minemod/scene/scene_session.h"
#include "pipelines/shader_module.hpp"
#include "shaders/program_check.hpp"
#include "shaders/scene_shader_contract.hpp"

namespace minemod::scene {

namespace {

constexpr renderer::SpirvLimits program_limits{MINEMOD_SCENE_MAX_PROGRAM_WORDS, std::uint32_t{1} << 16};

renderer::BatchProgram handles_of(const SceneProgram& program) noexcept {
    return renderer::BatchProgram{program.vertex.get(), program.fragment.get()};
}

}

minemod_result ProgramReload::begin() {
    if (staging_) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    staging_.emplace();
    is_refused_ = false;
    report_.clear();
    return MINEMOD_RESULT_OK;
}

minemod_result ProgramReload::stage(
    std::uint32_t program, std::span<const std::uint32_t> vertex, std::span<const std::uint32_t> fragment) {
    if (!staging_ || program == 0 || staging_->contains(program)) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    if (staging_->size() == MINEMOD_SCENE_MAX_PROGRAMS) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    renderer::ProgramCheck check =
        renderer::check_program(renderer::scene_shader_contract(), vertex, fragment, program_limits);
    if (!check.program) {
        for (const std::string& violation : check.violations) {
            refuse(program, violation);
        }
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    staging_->emplace(
        program,
        SceneProgram{
            renderer::create_shader_module(device_, check.program->vertex),
            renderer::create_shader_module(device_, check.program->fragment),
        });
    return MINEMOD_RESULT_OK;
}

minemod_result ProgramReload::abort() {
    if (!staging_) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    staging_.reset();
    return MINEMOD_RESULT_OK;
}

minemod_result ProgramReload::commit(
    std::uint64_t last_frame_of_current, renderer::BatchRenderer& batches, renderer::RetirementQueue& retirements) {
    if (!staging_) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    ScenePrograms staged = std::move(*staging_);
    staging_.reset();
    if (is_refused_) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }

    std::vector<renderer::DeviceHandle<VkPipeline>> replaced_pipelines;
    for (const auto& [id, program] : generations_.current()) {
        std::vector<renderer::DeviceHandle<VkPipeline>> pipelines = batches.take_pipelines_of(handles_of(program));
        replaced_pipelines.insert(
            replaced_pipelines.end(),
            std::make_move_iterator(pipelines.begin()),
            std::make_move_iterator(pipelines.end()));
    }
    retirements.retire(last_frame_of_current, std::move(replaced_pipelines));
    generations_.stage(std::move(staged));
    generations_.publish(last_frame_of_current, retirements);
    return MINEMOD_RESULT_OK;
}

std::optional<renderer::BatchProgram> ProgramReload::published(std::uint32_t program) const {
    const auto found = generations_.current().find(program);
    if (found == generations_.current().end()) {
        return std::nullopt;
    }
    return handles_of(found->second);
}

void ProgramReload::refuse(std::uint32_t program, const std::string& reason) {
    is_refused_ = true;
    report_ += "program " + std::to_string(program) + ": " + reason + '\n';
}

}
