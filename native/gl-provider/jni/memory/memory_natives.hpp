#pragma once

#include <jni.h>

namespace minemod::provider::jni {

bool register_memory_natives(JNIEnv& env) noexcept;

}
