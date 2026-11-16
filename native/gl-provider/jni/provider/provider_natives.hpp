#pragma once

#include <jni.h>

namespace minemod::provider::jni {

bool register_provider_natives(JNIEnv& env) noexcept;

}
