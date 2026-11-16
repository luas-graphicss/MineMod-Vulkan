#pragma once

#include <cstdint>
#include <span>
#include <string>

#include "minemod/scene/scene_session.h"

namespace minemod::scene::test_support {

minemod_result stage_program(
    minemod_scene_session* session,
    std::uint32_t program,
    std::span<const std::uint32_t> vertex,
    std::span<const std::uint32_t> fragment);

std::string reload_text(const minemod_scene_session* session);
std::string metrics_text(const minemod_scene_session* session);
minemod_scene_metrics metrics_of(const minemod_scene_session* session);

}
