#include "fields/long_fields.hpp"

namespace minemod::jni {

bool store_fields(JNIEnv& env, jlongArray target, std::span<const jlong> fields) noexcept {
    const auto field_count = static_cast<jsize>(fields.size());
    if (target == nullptr || env.GetArrayLength(target) != field_count) {
        return false;
    }
    env.SetLongArrayRegion(target, 0, field_count, fields.data());
    return true;
}

}
