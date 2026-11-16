#include "metrics/scene_metrics_calls.hpp"

#include <array>
#include <cstdint>

#include "arrays/native_text.hpp"
#include "fields/long_fields.hpp"
#include "library/native_registration.hpp"
#include "minemod/scene/scene_session.h"
#include "scene/scene_addresses.hpp"

namespace minemod::jni {

namespace {

jint JNICALL read_metrics(JNIEnv* env, jclass, jlong session, jlongArray fields) {
    minemod_scene_metrics metrics{};
    const minemod_result read =
        minemod_scene_session_get_metrics(object_at<minemod_scene_session>(session), &metrics);
    if (read != MINEMOD_RESULT_OK) {
        return read;
    }
    const std::array<jlong, 8> metric_fields{
        static_cast<jlong>(metrics.frames_measured),
        static_cast<jlong>(metrics.mean_frame_nanoseconds),
        static_cast<jlong>(metrics.worst_frame_nanoseconds),
        static_cast<jlong>(metrics.peak_texture_bytes),
        static_cast<jlong>(metrics.peak_staging_bytes),
        static_cast<jlong>(metrics.peak_pipelines),
        static_cast<jlong>(metrics.retired_resources),
        static_cast<jlong>(metrics.resource_generation),
    };
    return store_fields(*env, fields, metric_fields) ? MINEMOD_RESULT_OK : MINEMOD_RESULT_INVALID_ARGUMENT;
}

jbyteArray JNICALL report(JNIEnv* env, jclass, jlong session) {
    const minemod_scene_session* opened = object_at<minemod_scene_session>(session);
    return text_bytes(*env, [opened](char* destination, std::uint64_t capacity) {
        return minemod_scene_session_read_metrics_text(opened, destination, capacity);
    });
}

}

bool register_scene_metrics_calls(JNIEnv& env) noexcept {
    const std::array methods{
        native_method("nativeReadMetrics", "(J[J)I", &read_metrics),
        native_method("nativeReport", "(J)[B", &report),
    };
    return register_natives(env, "io/github/luasgraphicss/minemod/bridge/SceneDiagnostics", methods);
}

}
