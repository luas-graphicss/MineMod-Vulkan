#pragma once

#include <cstddef>
#include <span>

#include <jni.h>

namespace minemod::jni {

bool holds_fields(JNIEnv& env, jlongArray target, std::size_t field_count) noexcept;
bool store_fields(JNIEnv& env, jlongArray target, std::span<const jlong> fields) noexcept;

}
