#include "arrays/java_arrays.hpp"

#include <limits>

namespace minemod::jni {

namespace {

constexpr std::size_t max_array_length = static_cast<std::size_t>(std::numeric_limits<jsize>::max());

}

std::optional<std::string> copied_bytes(JNIEnv& env, jbyteArray bytes, std::size_t max_bytes) {
    if (bytes == nullptr) {
        return std::nullopt;
    }
    const jsize length = env.GetArrayLength(bytes);
    if (static_cast<std::size_t>(length) > max_bytes) {
        return std::nullopt;
    }
    std::string copied(static_cast<std::size_t>(length), '\0');
    env.GetByteArrayRegion(bytes, 0, length, reinterpret_cast<jbyte*>(copied.data()));
    return copied;
}

std::optional<std::vector<std::uint32_t>> copied_words(JNIEnv& env, jintArray words, std::size_t max_words) {
    if (words == nullptr) {
        return std::nullopt;
    }
    const jsize length = env.GetArrayLength(words);
    if (static_cast<std::size_t>(length) > max_words) {
        return std::nullopt;
    }
    std::vector<std::uint32_t> copied(static_cast<std::size_t>(length));
    env.GetIntArrayRegion(words, 0, length, reinterpret_cast<jint*>(copied.data()));
    return copied;
}

jbyteArray new_bytes(JNIEnv& env, std::string_view bytes) noexcept {
    if (bytes.size() > max_array_length) {
        return nullptr;
    }
    const auto length = static_cast<jsize>(bytes.size());
    const jbyteArray array = env.NewByteArray(length);
    if (array != nullptr) {
        env.SetByteArrayRegion(array, 0, length, reinterpret_cast<const jbyte*>(bytes.data()));
    }
    return array;
}

jintArray new_words(JNIEnv& env, std::span<const std::uint32_t> words) noexcept {
    if (words.size() > max_array_length) {
        return nullptr;
    }
    const auto length = static_cast<jsize>(words.size());
    const jintArray array = env.NewIntArray(length);
    if (array != nullptr) {
        env.SetIntArrayRegion(array, 0, length, reinterpret_cast<const jint*>(words.data()));
    }
    return array;
}

}
