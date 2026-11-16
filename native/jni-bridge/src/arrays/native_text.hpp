#pragma once

#include <algorithm>
#include <cstddef>
#include <cstdint>
#include <string>

#include <jni.h>

#include "arrays/java_arrays.hpp"

namespace minemod::jni {

template <typename Read>
jbyteArray text_bytes(JNIEnv& env, Read read) noexcept {
    try {
        std::string text(static_cast<std::size_t>(read(nullptr, 0)), '\0');
        const std::uint64_t available = read(text.data(), text.size());
        text.resize(static_cast<std::size_t>(std::min<std::uint64_t>(available, text.size())));
        return new_bytes(env, text);
    } catch (...) {
        return nullptr;
    }
}

}
