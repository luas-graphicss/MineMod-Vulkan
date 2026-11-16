#include <jni.h>

#include "bridge/bridge_calls.hpp"
#include "scene/scene_calls.hpp"
#include "session/session_calls.hpp"

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }
    const bool registered = minemod::jni::register_bridge_calls(*env) && minemod::jni::register_session_calls(*env)
        && minemod::jni::register_scene_calls(*env);
    return registered ? JNI_VERSION_1_8 : JNI_ERR;
}
