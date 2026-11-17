#include <exception>

#include "errors/java_exception.hpp"

namespace minemod::provider::jni {

namespace {

constexpr const char* exception_class =
    "io/github/luasgraphicss/minemod/compatibility/natives/binding/NativeProviderException";

}

void throw_provider_exception(JNIEnv& env, const char* message) noexcept {
    if (env.ExceptionCheck() == JNI_TRUE) {
        return;
    }
    const jclass exception = env.FindClass(exception_class);
    if (exception != nullptr) {
        env.ThrowNew(exception, message);
        env.DeleteLocalRef(exception);
    }
}

}
