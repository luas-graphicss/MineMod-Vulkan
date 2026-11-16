#pragma once

#include <cstdint>
#include <optional>

#include <jni.h>

#include "minemod/abi/version.h"

namespace minemod::jni {

inline constexpr jint max_version_component = 0xFFFF;

constexpr std::optional<minemod_abi_version> abi_version_from(jint major, jint minor) noexcept {
    if (major < 0 || major > max_version_component || minor < 0 || minor > max_version_component) {
        return std::nullopt;
    }
    return minemod_abi_version{static_cast<std::uint16_t>(major), static_cast<std::uint16_t>(minor)};
}

}
