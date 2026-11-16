#include "shaders/shader_calls.hpp"

#include <array>

#include "library/native_registration.hpp"

namespace minemod::jni {

namespace {

jboolean JNICALL is_available(JNIEnv*, jclass) {
    return JNI_FALSE;
}

}

bool register_shader_calls(JNIEnv& env) noexcept {
    const std::array methods{
        native_method("nativeIsAvailable", "()Z", &is_available),
    };
    return register_natives(env, "io/github/luasgraphicss/minemod/bridge/NativeShaderService", methods);
}

}
