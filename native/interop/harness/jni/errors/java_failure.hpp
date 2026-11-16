#pragma once

#include <exception>

#include <jni.h>

namespace minemod::interop::harness::jni {

inline void throw_illegal_state(JNIEnv& env, const char* message) noexcept {
    if (env.ExceptionCheck() == JNI_TRUE) {
        return;
    }
    const jclass failure = env.FindClass("java/lang/IllegalStateException");
    if (failure != nullptr) {
        env.ThrowNew(failure, message);
        env.DeleteLocalRef(failure);
    }
}

template <typename Answer, typename Operation>
Answer guarded(JNIEnv& env, Answer on_failure, Operation&& operation) noexcept {
    try {
        return operation();
    } catch (const std::exception& failure) {
        throw_illegal_state(env, failure.what());
    } catch (...) {
        throw_illegal_state(env, "the stand-in renderer failed for an unknown reason");
    }
    return on_failure;
}

template <typename Operation>
void guarded(JNIEnv& env, Operation&& operation) noexcept {
    guarded(env, 0, [&operation] {
        operation();
        return 0;
    });
}

}
