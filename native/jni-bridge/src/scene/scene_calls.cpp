#include "scene/scene_calls.hpp"

#include "scene/scene_host_calls.hpp"
#include "scene/scene_session_calls.hpp"

namespace minemod::jni {

bool register_scene_calls(JNIEnv& env) noexcept {
    return register_scene_host_calls(env) && register_scene_session_calls(env);
}

bool draws_scenes() noexcept {
    return true;
}

}
