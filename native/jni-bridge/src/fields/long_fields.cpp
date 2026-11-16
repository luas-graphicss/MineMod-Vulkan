#include "fields/long_fields.hpp"

namespace minemod::jni {

bool holds_fields(JNIEnv& env, jlongArray target, std::size_t field_count) noexcept {
    return target != nullptr && env.GetArrayLength(target) == static_cast<jsize>(field_count);
}

bool store_fields(JNIEnv& env, jlongArray target, std::span<const jlong> fields) noexcept {
    if (!holds_fields(env, target, fields.size())) {
        return false;
    }
    env.SetLongArrayRegion(target, 0, static_cast<jsize>(fields.size()), fields.data());
    return true;
}

}
