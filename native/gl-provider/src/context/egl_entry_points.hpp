#pragma once

#include <cstdint>

#include "library/provider_library.hpp"

namespace minemod::provider {

using EglHandle = void*;
using EglFunction = void (*)();

struct EglEntryPoints {
    EglHandle (*get_platform_display)(std::uint32_t platform, void* native_display, const std::intptr_t* attributes);
    std::uint32_t (*initialize)(EglHandle display, std::int32_t* major, std::int32_t* minor);
    std::uint32_t (*terminate)(EglHandle display);
    const char* (*query_string)(EglHandle display, std::int32_t name);
    std::uint32_t (*bind_api)(std::uint32_t api);
    EglHandle (*create_context)(EglHandle display, EglHandle config, EglHandle shared, const std::int32_t* attributes);
    std::uint32_t (*destroy_context)(EglHandle display, EglHandle context);
    std::uint32_t (*make_current)(EglHandle display, EglHandle draw, EglHandle read, EglHandle context);
    std::int32_t (*get_error)();
    EglFunction (*get_proc_address)(const char* name);
};

EglEntryPoints resolve_egl_entry_points(const ProviderLibrary& library);

}
