#include "reload/scene_reload_calls.hpp"

#include <array>
#include <cstdint>
#include <optional>
#include <vector>

#include "arrays/java_arrays.hpp"
#include "arrays/native_text.hpp"
#include "library/native_registration.hpp"
#include "minemod/scene/scene_session.h"
#include "scene/scene_addresses.hpp"

namespace minemod::jni {

namespace {

jint JNICALL begin(JNIEnv*, jclass, jlong session) {
    return minemod_scene_session_begin_reload(object_at<minemod_scene_session>(session));
}

jint JNICALL stage_program(JNIEnv* env, jclass, jlong session, jint program, jintArray vertex, jintArray fragment) {
    if (program < 0 || vertex == nullptr || fragment == nullptr) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    try {
        const std::optional<std::vector<std::uint32_t>> vertex_words =
            copied_words(*env, vertex, MINEMOD_SCENE_MAX_PROGRAM_WORDS);
        const std::optional<std::vector<std::uint32_t>> fragment_words =
            copied_words(*env, fragment, MINEMOD_SCENE_MAX_PROGRAM_WORDS);
        if (!vertex_words || !fragment_words) {
            return MINEMOD_RESULT_LIMIT_EXCEEDED;
        }
        return minemod_scene_session_stage_program(
            object_at<minemod_scene_session>(session),
            static_cast<std::uint32_t>(program),
            vertex_words->data(),
            vertex_words->size(),
            fragment_words->data(),
            fragment_words->size());
    } catch (...) {
        return MINEMOD_RESULT_OUT_OF_MEMORY;
    }
}

jint JNICALL commit(JNIEnv*, jclass, jlong session) {
    return minemod_scene_session_commit_reload(object_at<minemod_scene_session>(session));
}

jint JNICALL abort_reload(JNIEnv*, jclass, jlong session) {
    return minemod_scene_session_abort_reload(object_at<minemod_scene_session>(session));
}

jbyteArray JNICALL report(JNIEnv* env, jclass, jlong session) {
    const minemod_scene_session* opened = object_at<minemod_scene_session>(session);
    return text_bytes(*env, [opened](char* destination, std::uint64_t capacity) {
        return minemod_scene_session_read_reload_text(opened, destination, capacity);
    });
}

}

bool register_scene_reload_calls(JNIEnv& env) noexcept {
    const std::array methods{
        native_method("nativeBegin", "(J)I", &begin),
        native_method("nativeStageProgram", "(JI[I[I)I", &stage_program),
        native_method("nativeCommit", "(J)I", &commit),
        native_method("nativeAbort", "(J)I", &abort_reload),
        native_method("nativeReport", "(J)[B", &report),
    };
    return register_natives(env, "io/github/luasgraphicss/minemod/bridge/SceneReload", methods);
}

}
