#pragma once

#include <jni.h>

namespace minemod::provider::jni {

bool bind_java_interceptor(JavaVM& vm, JNIEnv& env) noexcept;

}
