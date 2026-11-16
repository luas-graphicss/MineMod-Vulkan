#include <dlfcn.h>

#include <array>
#include <cstdint>
#include <cstring>

#include "fake_provider/fake_calls.hpp"

#define FAKE_EXPORT extern "C" __attribute__((visibility("default")))

FAKE_EXPORT std::uint32_t glGetError();

namespace {

constexpr std::uint32_t platform_surfaceless = 0x31DD;
constexpr std::int32_t success = 0x3000;
constexpr std::int32_t bad_match = 0x3009;
constexpr std::int32_t bad_parameter = 0x300C;
constexpr std::int32_t major_version = 0x3098;
constexpr std::int32_t minor_version = 0x30FB;
constexpr std::int32_t profile_mask = 0x30FD;
constexpr std::int32_t none = 0x3038;
constexpr std::int32_t newest_major = 4;

int display_storage = 0;
std::array<FakeContextRequest, 8> contexts{};
std::size_t context_count = 0;
std::int32_t last_error = success;
thread_local void* current_context = nullptr;

}

FAKE_EXPORT const FakeContextRequest* fake_provider_context_request(void* context) {
    return static_cast<const FakeContextRequest*>(context);
}

FAKE_EXPORT void* fake_provider_current_context() {
    return current_context;
}

FAKE_EXPORT void* eglGetPlatformDisplay(std::uint32_t platform, void*, const std::intptr_t*) {
    if (platform != platform_surfaceless) {
        last_error = bad_parameter;
        return nullptr;
    }
    return &display_storage;
}

FAKE_EXPORT std::uint32_t eglInitialize(void*, std::int32_t* major, std::int32_t* minor) {
    *major = 1;
    *minor = 5;
    return 1;
}

FAKE_EXPORT std::uint32_t eglTerminate(void*) {
    return 1;
}

FAKE_EXPORT const char* eglQueryString(void* display, std::int32_t) {
    return display == nullptr
        ? "EGL_EXT_client_extensions EGL_MESA_platform_surfaceless"
        : "EGL_KHR_create_context EGL_KHR_no_config_context EGL_KHR_surfaceless_context";
}

FAKE_EXPORT std::uint32_t eglBindAPI(std::uint32_t) {
    return 1;
}

FAKE_EXPORT void* eglCreateContext(void*, void*, void* shared, const std::int32_t* attributes) {
    FakeContextRequest request{0, 0, 0, shared};
    for (const std::int32_t* attribute = attributes; *attribute != none; attribute += 2) {
        if (attribute[0] == major_version) {
            request.major = attribute[1];
        } else if (attribute[0] == minor_version) {
            request.minor = attribute[1];
        } else if (attribute[0] == profile_mask) {
            request.profile_mask = attribute[1];
        }
    }
    if (request.major > newest_major || context_count == contexts.size()) {
        last_error = bad_match;
        return nullptr;
    }
    contexts[context_count] = request;
    return &contexts[context_count++];
}

FAKE_EXPORT std::uint32_t eglDestroyContext(void*, void*) {
    return 1;
}

FAKE_EXPORT std::uint32_t eglMakeCurrent(void*, void*, void*, void* context) {
    current_context = context;
    return 1;
}

FAKE_EXPORT std::int32_t eglGetError() {
    const std::int32_t error = last_error;
    last_error = success;
    return error;
}

FAKE_EXPORT void (*eglGetProcAddress(const char* name))() {
    if (std::strcmp(name, "glGetError") == 0) {
        return reinterpret_cast<void (*)()>(&glGetError);
    }
    return nullptr;
}
