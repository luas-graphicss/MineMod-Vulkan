#pragma once

#include <jni.h>

namespace minemod::jni {

bool register_scene_calls(JNIEnv& env) noexcept;
bool draws_scenes() noexcept;

}
