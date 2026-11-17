#pragma once

#include <jni.h>

namespace minemod::provider::jni {

bool register_lwjgl2_natives(JNIEnv& env) noexcept;

}
