#pragma once

#include <jni.h>

namespace minemod::provider::jni {

bool register_call_natives(JNIEnv& env) noexcept;

}
