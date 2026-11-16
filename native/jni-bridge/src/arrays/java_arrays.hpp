#pragma once

#include <cstddef>
#include <cstdint>
#include <optional>
#include <span>
#include <string>
#include <string_view>
#include <vector>

#include <jni.h>

namespace minemod::jni {

std::optional<std::string> copied_bytes(JNIEnv& env, jbyteArray bytes, std::size_t max_bytes);
std::optional<std::vector<std::uint32_t>> copied_words(JNIEnv& env, jintArray words, std::size_t max_words);
jbyteArray new_bytes(JNIEnv& env, std::string_view bytes) noexcept;
bool store_bytes(JNIEnv& env, jbyteArray target, std::span<const std::uint8_t> bytes) noexcept;
jintArray new_words(JNIEnv& env, std::span<const std::uint32_t> words) noexcept;

}
