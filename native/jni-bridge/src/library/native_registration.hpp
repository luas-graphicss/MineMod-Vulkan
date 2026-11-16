#pragma once

#include <span>

#include <jni.h>

namespace minemod::jni {

template <typename Function>
JNINativeMethod native_method(const char* name, const char* signature, Function* function) noexcept {
    return JNINativeMethod{const_cast<char*>(name), const_cast<char*>(signature), reinterpret_cast<void*>(function)};
}

bool register_natives(JNIEnv& env, const char* owner_class, std::span<const JNINativeMethod> methods) noexcept;

}
