#include "scene/scene_calls.hpp"

#include "metrics/scene_metrics_calls.hpp"
#include "reload/scene_reload_calls.hpp"
#include "scene/scene_host_calls.hpp"
#include "scene/scene_session_calls.hpp"
#include "sharing/scene_sharing_calls.hpp"
#include "window/awt_surface_calls.hpp"

namespace minemod::jni {

bool register_scene_calls(JNIEnv& env) noexcept {
    return register_scene_host_calls(env) && register_scene_session_calls(env) && register_scene_reload_calls(env)
        && register_scene_metrics_calls(env) && register_scene_sharing_calls(env)
        && register_awt_surface_calls(env);
}

bool draws_scenes() noexcept {
    return true;
}

}
