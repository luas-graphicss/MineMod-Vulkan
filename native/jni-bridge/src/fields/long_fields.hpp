#pragma once

#include <span>

#include <jni.h>

namespace minemod::jni {

bool store_fields(JNIEnv& env, jlongArray target, std::span<const jlong> fields) noexcept;

}
