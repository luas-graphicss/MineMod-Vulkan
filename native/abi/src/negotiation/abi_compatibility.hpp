#pragma once

#include "minemod/abi/capabilities.h"
#include "minemod/abi/version.h"

namespace minemod::abi {

inline constexpr minemod_abi_version library_version{MINEMOD_ABI_VERSION_MAJOR, MINEMOD_ABI_VERSION_MINOR};

inline constexpr minemod_capabilities supported_capabilities =
    MINEMOD_CAPABILITY_COMMAND_STREAM | MINEMOD_CAPABILITY_BUFFER_RESOURCES;

constexpr bool accepts_client(minemod_abi_version client) noexcept {
    if (client.major != library_version.major) {
        return false;
    }
    const bool is_unstable_generation = library_version.major == 0;
    return is_unstable_generation ? client.minor == library_version.minor : client.minor <= library_version.minor;
}

}
