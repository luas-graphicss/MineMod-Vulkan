#include "scene/scene_calls.hpp"

namespace minemod::jni {

bool register_scene_calls(JNIEnv&) noexcept {
    return true;
}

bool draws_scenes() noexcept {
    return false;
}

}
