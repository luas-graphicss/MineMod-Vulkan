#pragma once

#include <jni.h>

namespace minemod::interop::harness::jni {

bool register_host_natives(JNIEnv& env) noexcept;

}
