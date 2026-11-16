#pragma once

#include <cstdint>
#include <optional>
#include <span>

#include <jni.h>

namespace minemod::jni {

std::optional<std::span<std::uint8_t>> direct_range(JNIEnv& env, jobject buffer, jint offset, jint length) noexcept;

}
