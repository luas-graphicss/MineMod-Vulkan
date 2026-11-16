#include "session/support/session_reload.hpp"

#include <stdexcept>

namespace minemod::scene::test_support {

minemod_result stage_program(
    minemod_scene_session* session,
    std::uint32_t program,
    std::span<const std::uint32_t> vertex,
    std::span<const std::uint32_t> fragment) {
    return minemod_scene_session_stage_program(
        session, program, vertex.data(), vertex.size(), fragment.data(), fragment.size());
}

std::string reload_text(const minemod_scene_session* session) {
    std::string text(minemod_scene_session_read_reload_text(session, nullptr, 0), '\0');
    minemod_scene_session_read_reload_text(session, text.data(), text.size());
    return text;
}

std::string metrics_text(const minemod_scene_session* session) {
    std::string text(minemod_scene_session_read_metrics_text(session, nullptr, 0), '\0');
    minemod_scene_session_read_metrics_text(session, text.data(), text.size());
    return text;
}

minemod_scene_metrics metrics_of(const minemod_scene_session* session) {
    minemod_scene_metrics metrics{};
    if (minemod_scene_session_get_metrics(session, &metrics) != MINEMOD_RESULT_OK) {
        throw std::runtime_error("reading the scene metrics failed");
    }
    return metrics;
}

}
