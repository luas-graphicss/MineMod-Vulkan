#ifndef MINEMOD_SCENE_SESSION_H
#define MINEMOD_SCENE_SESSION_H

#include <stdint.h>

#include "minemod/abi/command_stream.h"
#include "minemod/abi/linkage.h"
#include "minemod/abi/result.h"
#include "minemod/abi/version.h"

#define MINEMOD_SCENE_HOST_VALIDATION (UINT32_C(1) << 0)
#define MINEMOD_SCENE_HOST_MAX_EXTENSIONS UINT32_C(32)

#define MINEMOD_SCENE_MAX_EXTENT UINT32_C(16384)
#define MINEMOD_SCENE_MIN_ARENA_BYTES (UINT64_C(1) << 12)
#define MINEMOD_SCENE_MAX_ARENA_BYTES (UINT64_C(1) << 30)
#define MINEMOD_SCENE_MAX_TEXTURES (UINT32_C(1) << 16)
#define MINEMOD_SCENE_MAX_SECTIONS (UINT32_C(1) << 20)

enum {
    MINEMOD_PRESENTATION_OFFSCREEN = 0,
    MINEMOD_PRESENTATION_AWAITING_SWAPCHAIN = 1,
    MINEMOD_PRESENTATION_RUNNING = 2,
    MINEMOD_PRESENTATION_SUSPENDED = 3,
    MINEMOD_PRESENTATION_DEVICE_LOST = 4,
    MINEMOD_PRESENTATION_STOPPED = 5
};

enum {
    MINEMOD_PRESENT_PACING_VSYNC = 0,
    MINEMOD_PRESENT_PACING_IMMEDIATE = 1
};

typedef struct minemod_scene_host minemod_scene_host;
typedef struct minemod_scene_session minemod_scene_session;

typedef struct minemod_scene_host_config {
    uint32_t struct_bytes;
    minemod_abi_version abi_version;
    uint32_t flags;
    uint32_t extension_count;
    const char* const* extensions;
} minemod_scene_host_config;

typedef struct minemod_scene_session_config {
    uint32_t struct_bytes;
    uint32_t width;
    uint32_t height;
    uint32_t max_textures;
    uint32_t max_sections;
    uint32_t reserved;
    uint64_t window_surface;
    uint64_t upload_bytes_per_frame;
    uint64_t geometry_bytes_per_frame;
} minemod_scene_session_config;

typedef struct minemod_scene_counters {
    uint64_t frames_drawn;
    uint64_t frames_presented;
    uint64_t batches_drawn;
    uint64_t sections_published;
    uint64_t sections_dropped;
    uint64_t live_sections;
    uint64_t live_textures;
    uint64_t live_texture_bytes;
    uint64_t peak_upload_bytes;
    uint64_t peak_geometry_bytes;
} minemod_scene_counters;

MINEMOD_ABI_BEGIN

minemod_result minemod_scene_host_create(
    const minemod_scene_host_config* config, minemod_scene_host** host) MINEMOD_ABI_NOEXCEPT;

void minemod_scene_host_destroy(minemod_scene_host* host) MINEMOD_ABI_NOEXCEPT;

uint64_t minemod_scene_host_vulkan_instance(const minemod_scene_host* host) MINEMOD_ABI_NOEXCEPT;

uint64_t minemod_scene_host_read_validation_text(
    const minemod_scene_host* host, char* destination, uint64_t capacity) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_scene_session_create(
    minemod_scene_host* host,
    const minemod_scene_session_config* config,
    minemod_scene_session** session) MINEMOD_ABI_NOEXCEPT;

void minemod_scene_session_destroy(minemod_scene_session* session) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_scene_session_submit(
    minemod_scene_session* session,
    const uint8_t* bytes,
    uint64_t length,
    minemod_stream_report* report) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_scene_session_resize(
    minemod_scene_session* session, uint32_t width, uint32_t height) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_scene_session_set_present_pacing(
    minemod_scene_session* session, uint32_t pacing) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_scene_session_get_presentation_state(
    const minemod_scene_session* session, uint32_t* state) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_scene_session_read_frame(
    minemod_scene_session* session, uint8_t* rgba, uint64_t capacity) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_scene_session_get_counters(
    const minemod_scene_session* session, minemod_scene_counters* counters) MINEMOD_ABI_NOEXCEPT;

MINEMOD_ABI_END

#endif
