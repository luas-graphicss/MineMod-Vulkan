#include "calls/java_interceptor.hpp"

#include <cstdint>
#include <span>

#include "calls/call_gate.hpp"

namespace minemod::provider::jni {

namespace {

constexpr const char* gate_class_name = "io/github/luasgraphicss/minemod/compatibility/natives/gate/CallGate";

JavaVM* java_vm = nullptr;
jclass gate_class = nullptr;
jmethodID intercept_method = nullptr;

std::int64_t call_java_gate(std::uint32_t function, std::span<const std::int64_t> words) noexcept {
    JNIEnv* env = nullptr;
    if (java_vm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_8) != JNI_OK) {
        return 0;
    }
    const auto count = static_cast<jsize>(words.size());
    const jlongArray packed = env->NewLongArray(count);
    if (packed == nullptr) {
        return 0;
    }
    env->SetLongArrayRegion(packed, 0, count, reinterpret_cast<const jlong*>(words.data()));
    const jlong answer = env->CallStaticLongMethod(gate_class, intercept_method, static_cast<jint>(function), packed);
    env->DeleteLocalRef(packed);
    return env->ExceptionCheck() == JNI_TRUE ? 0 : answer;
}

}

bool bind_java_interceptor(JavaVM& vm, JNIEnv& env) noexcept {
    const jclass local = env.FindClass(gate_class_name);
    if (local == nullptr) {
        return false;
    }
    gate_class = static_cast<jclass>(env.NewGlobalRef(local));
    env.DeleteLocalRef(local);
    if (gate_class == nullptr) {
        return false;
    }
    intercept_method = env.GetStaticMethodID(gate_class, "intercept", "(I[J)J");
    if (intercept_method == nullptr) {
        return false;
    }
    java_vm = &vm;
    bind_interceptor(&call_java_gate);
    return true;
}

}
