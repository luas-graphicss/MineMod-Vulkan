#include "session/support/running_session.hpp"

#include <cstdlib>
#include <stdexcept>
#include <utility>

#include "minemod/abi/version.h"

namespace minemod::scene::test_support {

namespace {

constexpr int channel_tolerance = 2;
constexpr std::uint64_t arena_bytes = std::uint64_t{1} << 20;
constexpr std::uint32_t resource_limit = 64;

bool is_close(int drawn, int expected) noexcept {
    return std::abs(drawn - expected) <= channel_tolerance;
}

void require_ok(minemod_result result, const char* operation) {
    if (result != MINEMOD_RESULT_OK) {
        throw std::runtime_error(std::string(operation) + " failed with " + minemod_result_name(result));
    }
}

}

bool DrawnFrame::shows(std::uint32_t x, std::uint32_t y, const Rgb& expected) const {
    const std::size_t first = (std::size_t{y} * width_ + x) * 4;
    return is_close(rgba_.at(first), expected.red) && is_close(rgba_.at(first + 1), expected.green)
        && is_close(rgba_.at(first + 2), expected.blue);
}

std::string DrawnFrame::describe(std::uint32_t x, std::uint32_t y) const {
    const std::size_t first = (std::size_t{y} * width_ + x) * 4;
    return "drew (" + std::to_string(rgba_.at(first)) + ", " + std::to_string(rgba_.at(first + 1)) + ", "
        + std::to_string(rgba_.at(first + 2)) + ") at " + std::to_string(x) + ", " + std::to_string(y);
}

minemod_scene_session_config offscreen_config(std::uint32_t width, std::uint32_t height) {
    return minemod_scene_session_config{
        .struct_bytes = sizeof(minemod_scene_session_config),
        .width = width,
        .height = height,
        .max_textures = resource_limit,
        .max_sections = resource_limit,
        .flags = 0,
        .window_surface = 0,
        .upload_bytes_per_frame = arena_bytes,
        .geometry_bytes_per_frame = arena_bytes,
    };
}

minemod_scene_host* create_validated_host(std::span<const char* const> window_system_extensions) {
    const minemod_scene_host_config config{
        .struct_bytes = sizeof(minemod_scene_host_config),
        .abi_version = {MINEMOD_ABI_VERSION_MAJOR, MINEMOD_ABI_VERSION_MINOR},
        .flags = MINEMOD_SCENE_HOST_VALIDATION,
        .extension_count = static_cast<std::uint32_t>(window_system_extensions.size()),
        .extensions = window_system_extensions.data(),
    };
    minemod_scene_host* host = nullptr;
    require_ok(minemod_scene_host_create(&config, &host), "creating the scene host");
    return host;
}

RunningSession::RunningSession(
    const minemod_scene_session_config& config, std::span<const char* const> window_system_extensions)
    : host_(create_validated_host(window_system_extensions)) {
    const minemod_result created = minemod_scene_session_create(host_, &config, &session_);
    if (created != MINEMOD_RESULT_OK) {
        minemod_scene_host_destroy(host_);
        require_ok(created, "creating the scene session");
    }
}

RunningSession::~RunningSession() {
    minemod_scene_session_destroy(session_);
    minemod_scene_host_destroy(host_);
}

minemod_result RunningSession::submit(const std::vector<std::uint8_t>& stream) {
    minemod_stream_report report{};
    return submit(stream, report);
}

minemod_result RunningSession::submit(const std::vector<std::uint8_t>& stream, minemod_stream_report& report) {
    return minemod_scene_session_submit(session_, stream.data(), stream.size(), &report);
}

DrawnFrame RunningSession::read_frame(std::uint32_t width, std::uint32_t height) {
    std::vector<std::uint8_t> rgba(std::size_t{width} * height * 4);
    require_ok(minemod_scene_session_read_frame(session_, rgba.data(), rgba.size()), "reading the frame");
    return DrawnFrame(width, std::move(rgba));
}

minemod_scene_counters RunningSession::counters() const {
    minemod_scene_counters counters{};
    require_ok(minemod_scene_session_get_counters(session_, &counters), "reading the counters");
    return counters;
}

std::string RunningSession::validation_text() const {
    std::string text(minemod_scene_host_read_validation_text(host_, nullptr, 0), '\0');
    minemod_scene_host_read_validation_text(host_, text.data(), text.size());
    return text;
}

}
