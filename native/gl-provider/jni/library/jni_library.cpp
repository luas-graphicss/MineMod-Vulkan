#include <jni.h>

#include "calls/call_natives.hpp"
#include "calls/java_interceptor.hpp"
#include "lwjgl2/lookup_natives.hpp"
#include "memory/memory_natives.hpp"
#include "provider/provider_natives.hpp"

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    using namespace minemod::provider::jni;
    JNIEnv* env = nullptr;
    if (vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return JNI_ERR;
    }
    const bool ready = register_provider_natives(*env) && register_call_natives(*env)
        && register_memory_natives(*env) && register_lwjgl2_natives(*env) && bind_java_interceptor(*vm, *env);
    return ready ? JNI_VERSION_1_8 : JNI_ERR;
}
