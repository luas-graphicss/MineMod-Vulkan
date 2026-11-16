#pragma once

#include <array>
#include <cstdint>
#include <span>
#include <string>
#include <vector>

#include "minemod/scene/scene_session.h"

namespace minemod::scene::test_support {

struct Rgb {
    int red;
    int green;
    int blue;
};

class DrawnFrame {
public:
    DrawnFrame(std::uint32_t width, std::vector<std::uint8_t> rgba) : width_(width), rgba_(std::move(rgba)) {}

    bool shows(std::uint32_t x, std::uint32_t y, const Rgb& expected) const;
    std::string describe(std::uint32_t x, std::uint32_t y) const;

private:
    std::uint32_t width_;
    std::vector<std::uint8_t> rgba_;
};

minemod_scene_session_config offscreen_config(std::uint32_t width, std::uint32_t height);

class RunningSession {
public:
    explicit RunningSession(
        const minemod_scene_session_config& config, std::span<const char* const> window_system_extensions = {});
    ~RunningSession();

    RunningSession(const RunningSession&) = delete;
    RunningSession& operator=(const RunningSession&) = delete;

    minemod_scene_session* get() const noexcept {
        return session_;
    }

    minemod_result submit(const std::vector<std::uint8_t>& stream);
    minemod_result submit(const std::vector<std::uint8_t>& stream, minemod_stream_report& report);
    DrawnFrame read_frame(std::uint32_t width, std::uint32_t height);
    minemod_scene_counters counters() const;
    std::string validation_text() const;

private:
    minemod_scene_host* host_ = nullptr;
    minemod_scene_session* session_ = nullptr;
};

minemod_scene_host* create_validated_host(std::span<const char* const> window_system_extensions);

}
