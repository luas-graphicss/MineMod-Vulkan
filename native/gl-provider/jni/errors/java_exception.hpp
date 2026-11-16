#pragma once

#include <jni.h>

namespace minemod::provider::jni {

void throw_provider_exception(JNIEnv& env, const char* message) noexcept;

template <typename Answer, typename Operation>
Answer guarded(JNIEnv& env, Answer on_failure, Operation&& operation) noexcept {
    try {
        return operation();
    } catch (const std::exception& failure) {
        throw_provider_exception(env, failure.what());
    } catch (...) {
        throw_provider_exception(env, "the native provider failed for an unknown reason");
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
