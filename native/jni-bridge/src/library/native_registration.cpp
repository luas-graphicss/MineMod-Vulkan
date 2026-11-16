#include "library/native_registration.hpp"

namespace minemod::jni {

bool register_natives(JNIEnv& env, const char* owner_class, std::span<const JNINativeMethod> methods) noexcept {
    const jclass owner = env.FindClass(owner_class);
    if (owner == nullptr) {
        return false;
    }
    const jint status = env.RegisterNatives(owner, methods.data(), static_cast<jint>(methods.size()));
    env.DeleteLocalRef(owner);
    return status == JNI_OK;
}

}
