#include <jni.h>

#include "host/host_natives.hpp"

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }
    return minemod::interop::harness::jni::register_host_natives(*env) ? JNI_VERSION_1_8 : JNI_ERR;
}
