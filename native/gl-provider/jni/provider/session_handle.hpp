#pragma once

#include <jni.h>

#include "session/provider_session.hpp"

namespace minemod::provider::jni {

inline ProviderSession& session_of(jlong handle) {
    if (handle == 0) {
        throw CallError("the provider was closed");
    }
    return *reinterpret_cast<ProviderSession*>(static_cast<std::uintptr_t>(handle));
}

}
