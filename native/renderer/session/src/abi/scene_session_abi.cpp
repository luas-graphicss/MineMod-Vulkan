#include "minemod/scene/scene_session.h"

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <cstring>
#include <memory>
#include <new>
#include <span>
#include <stdexcept>
#include <string>
#include <utility>
#include <vector>

#include <vulkan/vulkan.h>

#include "device/instance.hpp"
#include "device/vulkan_error.hpp"
#include "host/scene_host.hpp"
#include "minemod/abi/negotiation.h"
#include "session/scene_session.hpp"

static_assert(sizeof(minemod_scene_host_config) == 16 + sizeof(void*));
static_assert(sizeof(minemod_scene_session_config) == 48);
static_assert(sizeof(minemod_scene_counters) == 80);

struct minemod_scene_host {
    std::shared_ptr<const minemod::scene::SceneHost> core;
};

struct minemod_scene_session {
    minemod_scene_session(
        std::shared_ptr<const minemod::scene::SceneHost> host, const minemod::scene::SessionSetup& setup)
        : core(std::move(host), setup) {}

    minemod::scene::SceneSession core;
};

namespace {

using minemod::scene::SceneHost;
using minemod::scene::SceneSession;

template <typename Operation>
minemod_result run_guarded(Operation&& operation) noexcept {
    try {
        return operation();
    } catch (const std::bad_alloc&) {
        return MINEMOD_RESULT_OUT_OF_MEMORY;
    } catch (const std::length_error&) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    } catch (const std::invalid_argument&) {
        return MINEMOD_RESULT_MALFORMED_STREAM;
    } catch (const minemod::renderer::UnsupportedSystem&) {
        return MINEMOD_RESULT_UNSUPPORTED_CAPABILITY;
    } catch (...) {
        return MINEMOD_RESULT_INTERNAL_ERROR;
    }
}

minemod_result check_abi_version(minemod_abi_version version) noexcept {
    const minemod_negotiation_request request{
        .struct_bytes = sizeof(minemod_negotiation_request),
        .client_version = version,
        .required_capabilities = 0,
    };
    minemod_negotiation negotiation{};
    return minemod_negotiate(&request, &negotiation);
}

minemod_result check_host_config(const minemod_scene_host_config& config) noexcept {
    const bool lists_extensions = config.extension_count == 0 || config.extensions != nullptr;
    if (config.struct_bytes != sizeof(minemod_scene_host_config)
        || (config.flags & ~MINEMOD_SCENE_HOST_VALIDATION) != 0 || !lists_extensions) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    if (config.extension_count > MINEMOD_SCENE_HOST_MAX_EXTENSIONS) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    const std::span<const char* const> extensions(config.extensions, config.extension_count);
    if (std::ranges::any_of(extensions, [](const char* extension) { return extension == nullptr; })) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return check_abi_version(config.abi_version);
}

bool is_within(std::uint64_t value, std::uint64_t smallest, std::uint64_t largest) noexcept {
    return value >= smallest && value <= largest;
}

minemod_result check_session_config(const minemod_scene_session_config& config) noexcept {
    const bool has_area = config.width != 0 && config.height != 0;
    if (config.struct_bytes != sizeof(minemod_scene_session_config) || config.reserved != 0 || !has_area
        || config.max_textures == 0 || config.max_sections == 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const bool fits_limits = config.width <= MINEMOD_SCENE_MAX_EXTENT && config.height <= MINEMOD_SCENE_MAX_EXTENT
        && config.max_textures <= MINEMOD_SCENE_MAX_TEXTURES && config.max_sections <= MINEMOD_SCENE_MAX_SECTIONS
        && is_within(config.upload_bytes_per_frame, MINEMOD_SCENE_MIN_ARENA_BYTES, MINEMOD_SCENE_MAX_ARENA_BYTES)
        && is_within(config.geometry_bytes_per_frame, MINEMOD_SCENE_MIN_ARENA_BYTES, MINEMOD_SCENE_MAX_ARENA_BYTES);
    return fits_limits ? MINEMOD_RESULT_OK : MINEMOD_RESULT_LIMIT_EXCEEDED;
}

VkSurfaceKHR surface_from(std::uint64_t window_surface) noexcept {
#if VK_USE_64_BIT_PTR_DEFINES == 1
    return reinterpret_cast<VkSurfaceKHR>(static_cast<std::uintptr_t>(window_surface));
#else
    return static_cast<VkSurfaceKHR>(window_surface);
#endif
}

minemod::scene::SessionSetup setup_from(const minemod_scene_session_config& config) noexcept {
    return minemod::scene::SessionSetup{
        VkExtent2D{config.width, config.height},
        surface_from(config.window_surface),
        minemod::scene::SceneLimits{
            config.upload_bytes_per_frame,
            config.geometry_bytes_per_frame,
            config.max_textures,
            config.max_sections,
        },
    };
}

minemod_result run_command(void* session, const minemod_command_record* record) {
    auto& core = *static_cast<SceneSession*>(session);
    const std::span<const std::uint8_t> payload(record->payload, record->payload_bytes);
    return run_guarded([&] { return core.execute(record->opcode, std::as_bytes(payload)); });
}

}

minemod_result minemod_scene_host_create(
    const minemod_scene_host_config* config, minemod_scene_host** host) noexcept {
    if (config == nullptr || host == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    *host = nullptr;
    if (const minemod_result checked = check_host_config(*config); checked != MINEMOD_RESULT_OK) {
        return checked;
    }
    return run_guarded([&] {
        const bool validates = (config->flags & MINEMOD_SCENE_HOST_VALIDATION) != 0;
        const auto validation = validates ? minemod::renderer::Validation::required
                                          : minemod::renderer::Validation::disabled;
        const std::span<const char* const> extensions(config->extensions, config->extension_count);
        auto core = std::make_shared<const SceneHost>(validation, extensions);
        *host = std::make_unique<minemod_scene_host>(minemod_scene_host{std::move(core)}).release();
        return MINEMOD_RESULT_OK;
    });
}

void minemod_scene_host_destroy(minemod_scene_host* host) noexcept {
    std::unique_ptr<minemod_scene_host> owned(host);
}

uint64_t minemod_scene_host_vulkan_instance(const minemod_scene_host* host) noexcept {
    if (host == nullptr) {
        return 0;
    }
    return static_cast<std::uint64_t>(reinterpret_cast<std::uintptr_t>(host->core->instance()));
}

uint64_t minemod_scene_host_read_validation_text(
    const minemod_scene_host* host, char* destination, uint64_t capacity) noexcept {
    if (host == nullptr || (destination == nullptr && capacity != 0)) {
        return 0;
    }
    try {
        const std::string text = host->core->validation_text();
        const auto copied = static_cast<std::size_t>(std::min<std::uint64_t>(capacity, text.size()));
        if (copied != 0) {
            std::memcpy(destination, text.data(), copied);
        }
        return text.size();
    } catch (...) {
        return 0;
    }
}

minemod_result minemod_scene_session_create(
    minemod_scene_host* host,
    const minemod_scene_session_config* config,
    minemod_scene_session** session) noexcept {
    if (host == nullptr || config == nullptr || session == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    *session = nullptr;
    if (const minemod_result checked = check_session_config(*config); checked != MINEMOD_RESULT_OK) {
        return checked;
    }
    return run_guarded([&] {
        *session = std::make_unique<minemod_scene_session>(host->core, setup_from(*config)).release();
        return MINEMOD_RESULT_OK;
    });
}

void minemod_scene_session_destroy(minemod_scene_session* session) noexcept {
    std::unique_ptr<minemod_scene_session> owned(session);
}

minemod_result minemod_scene_session_submit(
    minemod_scene_session* session,
    const uint8_t* bytes,
    uint64_t length,
    minemod_stream_report* report) noexcept {
    if (session == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return minemod_visit_command_stream(bytes, length, run_command, &session->core, report);
}

minemod_result minemod_scene_session_resize(
    minemod_scene_session* session, uint32_t width, uint32_t height) noexcept {
    if (session == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    if (width > MINEMOD_SCENE_MAX_EXTENT || height > MINEMOD_SCENE_MAX_EXTENT) {
        return MINEMOD_RESULT_LIMIT_EXCEEDED;
    }
    return run_guarded([&] { return session->core.resize(VkExtent2D{width, height}); });
}

minemod_result minemod_scene_session_set_present_pacing(minemod_scene_session* session, uint32_t pacing) noexcept {
    if (session == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    switch (pacing) {
        case MINEMOD_PRESENT_PACING_VSYNC:
            session->core.set_present_pacing(minemod::renderer::PresentPacing::vsync);
            return MINEMOD_RESULT_OK;
        case MINEMOD_PRESENT_PACING_IMMEDIATE:
            session->core.set_present_pacing(minemod::renderer::PresentPacing::immediate);
            return MINEMOD_RESULT_OK;
        default:
            return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
}

minemod_result minemod_scene_session_get_presentation_state(
    const minemod_scene_session* session, uint32_t* state) noexcept {
    if (session == nullptr || state == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    *state = session->core.presentation_state();
    return MINEMOD_RESULT_OK;
}

minemod_result minemod_scene_session_read_frame(
    minemod_scene_session* session, uint8_t* rgba, uint64_t capacity) noexcept {
    if (session == nullptr || rgba == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    if (capacity > UINT64_C(4) * MINEMOD_SCENE_MAX_EXTENT * MINEMOD_SCENE_MAX_EXTENT) {
        return MINEMOD_RESULT_LENGTH_OUT_OF_BOUNDS;
    }
    const std::span<std::uint8_t> pixels(rgba, static_cast<std::size_t>(capacity));
    return run_guarded([&] { return session->core.read_frame(std::as_writable_bytes(pixels)); });
}

minemod_result minemod_scene_session_get_counters(
    const minemod_scene_session* session, minemod_scene_counters* counters) noexcept {
    if (session == nullptr || counters == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    *counters = session->core.counters();
    return MINEMOD_RESULT_OK;
}
