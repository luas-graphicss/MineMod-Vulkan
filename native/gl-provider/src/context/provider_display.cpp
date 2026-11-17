#include "context/provider_display.hpp"

#include <algorithm>
#include <array>
#include <cstdio>
#include <string>
#include <string_view>

namespace minemod::provider {

namespace {

constexpr std::uint32_t egl_platform_surfaceless = 0x31DD;
constexpr std::uint32_t egl_opengl_api = 0x30A2;
constexpr std::int32_t egl_extensions = 0x3055;
constexpr std::int32_t egl_context_major_version = 0x3098;
constexpr std::int32_t egl_context_minor_version = 0x30FB;
constexpr std::int32_t egl_context_profile_mask = 0x30FD;
constexpr std::int32_t egl_core_profile_bit = 0x1;
constexpr std::int32_t egl_compatibility_profile_bit = 0x2;
constexpr std::int32_t egl_none = 0x3038;

constexpr std::array<std::string_view, 1> client_requirements{"EGL_MESA_platform_surfaceless"};
constexpr std::array<std::string_view, 3> display_requirements{
    "EGL_KHR_create_context", "EGL_KHR_no_config_context", "EGL_KHR_surfaceless_context"};

bool lists_extension(std::string_view extensions, std::string_view wanted) noexcept {
    std::size_t start = 0;
    while (start < extensions.size()) {
        const std::size_t end = std::min(extensions.find(' ', start), extensions.size());
        if (extensions.substr(start, end - start) == wanted) {
            return true;
        }
        start = end + 1;
    }
    return false;
}

template <std::size_t Count>
void require_extensions(const char* listed, const std::array<std::string_view, Count>& required) {
    const std::string_view extensions = listed != nullptr ? listed : "";
    for (const std::string_view extension : required) {
        if (!lists_extension(extensions, extension)) {
            throw DisplayError("the provider does not offer " + std::string(extension));
        }
    }
}

}

ProviderDisplay::ProviderDisplay(const ProviderLibrary& library)
    : library_(library), egl_(resolve_egl_entry_points(library)) {
    require_extensions(egl_.query_string(nullptr, egl_extensions), client_requirements);
    display_ = egl_.get_platform_display(egl_platform_surfaceless, nullptr, nullptr);
    if (display_ == nullptr) {
        fail("opening the surfaceless display");
    }
    std::int32_t major = 0;
    std::int32_t minor = 0;
    if (egl_.initialize(display_, &major, &minor) == 0) {
        fail("initializing the display");
    }
    try {
        require_extensions(egl_.query_string(display_, egl_extensions), display_requirements);
        if (egl_.bind_api(egl_opengl_api) == 0) {
            fail("binding desktop OpenGL");
        }
    } catch (...) {
        egl_.terminate(display_);
        throw;
    }
}

ProviderDisplay::~ProviderDisplay() {
    egl_.terminate(display_);
}

EglHandle ProviderDisplay::create_context(const ContextRequest& request, EglHandle shared_with) const {
    const std::int32_t profile =
        request.profile == ContextProfile::core ? egl_core_profile_bit : egl_compatibility_profile_bit;
    const std::array<std::int32_t, 7> attributes{
        egl_context_major_version, request.major,
        egl_context_minor_version, request.minor,
        egl_context_profile_mask, profile,
        egl_none};
    const EglHandle context = egl_.create_context(display_, nullptr, shared_with, attributes.data());
    if (context == nullptr) {
        fail("creating the context");
    }
    return context;
}

void ProviderDisplay::destroy_context(EglHandle context) const noexcept {
    egl_.destroy_context(display_, context);
}

void ProviderDisplay::make_current(EglHandle context) const {
    if (egl_.make_current(display_, nullptr, nullptr, context) == 0) {
        fail("making the context current");
    }
}

void ProviderDisplay::release_current() const {
    make_current(nullptr);
}

void* ProviderDisplay::function_address(const char* name) const noexcept {
    const EglFunction function = egl_.get_proc_address(name);
    return function != nullptr ? reinterpret_cast<void*>(function) : library_.symbol(name);
}

void ProviderDisplay::fail(const char* operation) const {
    std::array<char, 16> code{};
    std::snprintf(code.data(), code.size(), "0x%04X", static_cast<unsigned int>(egl_.get_error()));
    throw DisplayError(std::string(operation) + " failed with EGL error " + code.data());
}

}
