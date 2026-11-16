#pragma once

#include <cstdint>

#include <jni.h>

namespace minemod::jni {

template <typename Object>
Object* object_at(jlong address) noexcept {
    return reinterpret_cast<Object*>(static_cast<std::intptr_t>(address));
}

template <typename Object>
jlong address_of(Object* object) noexcept {
    return static_cast<jlong>(reinterpret_cast<std::intptr_t>(object));
}

}
