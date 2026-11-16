#include <array>
#include <cstdint>
#include <string>
#include <vector>

#include <doctest.h>
#include <vulkan/vulkan.h>

#include "minemod/scene/scene_session.h"
#include "session/support/running_session.hpp"
#include "session/support/scene_shapes.hpp"
#include "session/support/scene_stream.hpp"

namespace {

using namespace minemod::scene::test_support;

constexpr VkExtent2D window_extent{64, 48};
constexpr VkExtent2D resized_extent{96, 48};
constexpr std::array headless_extensions{VK_KHR_SURFACE_EXTENSION_NAME, VK_EXT_HEADLESS_SURFACE_EXTENSION_NAME};
constexpr std::array<float, 4> black{0.0f, 0.0f, 0.0f, 1.0f};
constexpr Color red{255, 0, 0, 255};
constexpr VkRect2D corner{{0, 0}, {8, 8}};

class WindowedSession {
public:
    WindowedSession() : host_(create_validated_host(headless_extensions)) {
        minemod_scene_session_config config = offscreen_config(window_extent.width, window_extent.height);
        config.window_surface = create_surface();
        REQUIRE(minemod_scene_session_create(host_, &config, &session_) == MINEMOD_RESULT_OK);
    }

    ~WindowedSession() {
        minemod_scene_session_destroy(session_);
        minemod_scene_host_destroy(host_);
    }

    WindowedSession(const WindowedSession&) = delete;
    WindowedSession& operator=(const WindowedSession&) = delete;

    minemod_scene_session* get() const noexcept {
        return session_;
    }

    void draw_frame(VkExtent2D canvas) {
        ViewState view;
        view.projection = pixel_projection(canvas);
        const std::vector<std::uint8_t> stream = SceneStream()
                                                     .begin_frame(black, 0)
                                                     .set_view(view)
                                                     .draw_batch(BatchState{}, colored_rectangle(corner, red))
                                                     .end_frame()
                                                     .build();
        minemod_stream_report report{};
        REQUIRE(minemod_scene_session_submit(session_, stream.data(), stream.size(), &report) == MINEMOD_RESULT_OK);
    }

    std::uint64_t frames_presented() const {
        minemod_scene_counters counters{};
        REQUIRE(minemod_scene_session_get_counters(session_, &counters) == MINEMOD_RESULT_OK);
        return counters.frames_presented;
    }

    std::string validation_text() const {
        std::string text(minemod_scene_host_read_validation_text(host_, nullptr, 0), '\0');
        minemod_scene_host_read_validation_text(host_, text.data(), text.size());
        return text;
    }

private:
    std::uint64_t create_surface() const {
        const auto instance =
            reinterpret_cast<VkInstance>(static_cast<std::uintptr_t>(minemod_scene_host_vulkan_instance(host_)));
        const VkHeadlessSurfaceCreateInfoEXT create_info{.sType = VK_STRUCTURE_TYPE_HEADLESS_SURFACE_CREATE_INFO_EXT};
        VkSurfaceKHR surface = VK_NULL_HANDLE;
        REQUIRE(vkCreateHeadlessSurfaceEXT(instance, &create_info, nullptr, &surface) == VK_SUCCESS);
#if VK_USE_64_BIT_PTR_DEFINES == 1
        return static_cast<std::uint64_t>(reinterpret_cast<std::uintptr_t>(surface));
#else
        return static_cast<std::uint64_t>(surface);
#endif
    }

    minemod_scene_host* host_;
    minemod_scene_session* session_ = nullptr;
};

}

TEST_CASE("a session with a window surface presents every frame it draws") {
    WindowedSession session;

    for (int frame = 0; frame < 5; ++frame) {
        session.draw_frame(window_extent);
    }

    CHECK(session.frames_presented() == 5);
    CHECK(session.validation_text() == "");
}

TEST_CASE("a window without area keeps drawing and presents again once it has area") {
    WindowedSession session;
    session.draw_frame(window_extent);

    REQUIRE(minemod_scene_session_resize(session.get(), 0, 0) == MINEMOD_RESULT_OK);
    session.draw_frame(window_extent);
    CHECK(session.frames_presented() == 1);

    REQUIRE(
        minemod_scene_session_resize(session.get(), resized_extent.width, resized_extent.height) == MINEMOD_RESULT_OK);
    session.draw_frame(resized_extent);
    session.draw_frame(resized_extent);
    CHECK(session.frames_presented() == 3);
    CHECK(session.validation_text() == "");
}
