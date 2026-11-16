#include "lwjgl2/lookup_natives.hpp"

#include <array>
#include <cstdint>
#include <mutex>
#include <utility>

#include "errors/java_exception.hpp"
#include "library/binding_class.hpp"
#include "library/native_registration.hpp"

namespace minemod::provider::jni {
namespace {

constexpr const char* lookup_class_name =
    "io/github/luasgraphicss/minemod/compatibility/dispatch/install/FunctionLookup";

struct BoundLookup {
    jobject lookup = nullptr;
    jmethodID address_of = nullptr;
};

std::mutex bound_lookup_guard;
BoundLookup bound_lookup;

BoundLookup borrow_bound_lookup(JNIEnv& env) {
    const std::scoped_lock lock(bound_lookup_guard);
    if (bound_lookup.lookup == nullptr) {
        return BoundLookup{};
    }
    return BoundLookup{env.NewLocalRef(bound_lookup.lookup), bound_lookup.address_of};
}

void replace_bound_lookup(JNIEnv& env, BoundLookup replacement) {
    BoundLookup replaced;
    {
        const std::scoped_lock lock(bound_lookup_guard);
        replaced = std::exchange(bound_lookup, replacement);
    }
    if (replaced.lookup != nullptr) {
        env.DeleteGlobalRef(replaced.lookup);
    }
}

jlong function_address(JNIEnv* env, jclass, jlong name) noexcept {
    if (name == 0) {
        return 0;
    }
    const BoundLookup borrowed = borrow_bound_lookup(*env);
    if (borrowed.lookup == nullptr) {
        return 0;
    }
    const jstring function = env->NewStringUTF(reinterpret_cast<const char*>(static_cast<std::uintptr_t>(name)));
    jlong address = 0;
    if (function != nullptr) {
        address = env->CallLongMethod(borrowed.lookup, borrowed.address_of, function);
        env->DeleteLocalRef(function);
    }
    env->DeleteLocalRef(borrowed.lookup);
    return env->ExceptionCheck() == JNI_TRUE ? 0 : address;
}

void keep_system_library_unloaded(JNIEnv*, jclass) noexcept {}

void bind_lookup(JNIEnv* env, jclass, jclass gl_context, jobject lookup) noexcept {
    if (gl_context == nullptr || lookup == nullptr) {
        throw_provider_exception(*env, "the LWJGL 2 binding needs its GLContext class and a function lookup");
        return;
    }
    const jclass lookup_class = env->FindClass(lookup_class_name);
    if (lookup_class == nullptr) {
        return;
    }
    const jmethodID address_of = env->GetMethodID(lookup_class, "addressOf", "(Ljava/lang/String;)J");
    env->DeleteLocalRef(lookup_class);
    if (address_of == nullptr) {
        return;
    }
    const std::array methods{
        minemod::jni::native_method("ngetFunctionAddress", "(J)J", &function_address),
        minemod::jni::native_method("nLoadOpenGLLibrary", "()V", &keep_system_library_unloaded),
        minemod::jni::native_method("nUnloadOpenGLLibrary", "()V", &keep_system_library_unloaded),
    };
    if (env->RegisterNatives(gl_context, methods.data(), static_cast<jint>(methods.size())) != JNI_OK) {
        return;
    }
    const jobject kept = env->NewGlobalRef(lookup);
    if (kept == nullptr) {
        throw_provider_exception(*env, "the function lookup of the LWJGL 2 binding could not be kept");
        return;
    }
    replace_bound_lookup(*env, BoundLookup{kept, address_of});
}

void unbind_lookup(JNIEnv* env, jclass) noexcept {
    replace_bound_lookup(*env, BoundLookup{});
}

}

bool register_lwjgl2_natives(JNIEnv& env) noexcept {
    const std::array methods{
        minemod::jni::native_method(
            "bindLwjgl2Lookup",
            "(Ljava/lang/Class;Lio/github/luasgraphicss/minemod/compatibility/dispatch/install/FunctionLookup;)V",
            &bind_lookup),
        minemod::jni::native_method("unbindLwjgl2Lookup", "()V", &unbind_lookup),
    };
    return minemod::jni::register_natives(env, binding_class, methods);
}

}
