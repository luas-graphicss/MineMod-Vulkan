#include "scene/scene_session_calls.hpp"

#include <array>
#include <cstdint>

#include "buffers/direct_buffer.hpp"
#include "fields/long_fields.hpp"
#include "library/native_registration.hpp"
#include "minemod/scene/scene_session.h"
#include "scene/scene_addresses.hpp"
#include "stream/stream_report_fields.hpp"

namespace minemod::jni {

namespace {

jint JNICALL create(
    JNIEnv* env,
    jclass,
    jlong host,
    jint width,
    jint height,
    jint max_textures,
    jint max_sections,
    jlong window_surface,
    jlong upload_bytes_per_frame,
    jlong geometry_bytes_per_frame,
    jlongArray session_field) {
    const bool has_valid_counts = width >= 0 && height >= 0 && max_textures >= 0 && max_sections >= 0
        && upload_bytes_per_frame >= 0 && geometry_bytes_per_frame >= 0;
    if (!has_valid_counts) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const minemod_scene_session_config config{
        .struct_bytes = sizeof(minemod_scene_session_config),
        .width = static_cast<std::uint32_t>(width),
        .height = static_cast<std::uint32_t>(height),
        .max_textures = static_cast<std::uint32_t>(max_textures),
        .max_sections = static_cast<std::uint32_t>(max_sections),
        .reserved = 0,
        .window_surface = static_cast<std::uint64_t>(window_surface),
        .upload_bytes_per_frame = static_cast<std::uint64_t>(upload_bytes_per_frame),
        .geometry_bytes_per_frame = static_cast<std::uint64_t>(geometry_bytes_per_frame),
    };
    minemod_scene_session* session = nullptr;
    const minemod_result created =
        minemod_scene_session_create(object_at<minemod_scene_host>(host), &config, &session);
    if (created != MINEMOD_RESULT_OK) {
        return created;
    }
    const std::array<jlong, 1> address{address_of(session)};
    if (!store_fields(*env, session_field, address)) {
        minemod_scene_session_destroy(session);
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return MINEMOD_RESULT_OK;
}

void JNICALL destroy(JNIEnv*, jclass, jlong address) {
    minemod_scene_session_destroy(object_at<minemod_scene_session>(address));
}

jint JNICALL submit(JNIEnv* env, jclass, jlong address, jobject stream, jint offset, jint length, jlongArray fields) {
    minemod_stream_report report = rejected_stream_report();
    minemod_result result = MINEMOD_RESULT_INVALID_ARGUMENT;
    if (const auto bytes = direct_range(*env, stream, offset, length)) {
        result = minemod_scene_session_submit(
            object_at<minemod_scene_session>(address), bytes->data(), bytes->size(), &report);
    }
    return store_fields(*env, fields, stream_report_fields(report)) ? result : MINEMOD_RESULT_INVALID_ARGUMENT;
}

jint JNICALL resize(JNIEnv*, jclass, jlong address, jint width, jint height) {
    if (width < 0 || height < 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return minemod_scene_session_resize(
        object_at<minemod_scene_session>(address), static_cast<std::uint32_t>(width), static_cast<std::uint32_t>(height));
}

jint JNICALL set_pacing(JNIEnv*, jclass, jlong address, jint pacing) {
    if (pacing < 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return minemod_scene_session_set_present_pacing(
        object_at<minemod_scene_session>(address), static_cast<std::uint32_t>(pacing));
}

jint JNICALL read_presentation_state(JNIEnv* env, jclass, jlong address, jlongArray state_field) {
    std::uint32_t state = 0;
    const minemod_result read =
        minemod_scene_session_get_presentation_state(object_at<minemod_scene_session>(address), &state);
    if (read != MINEMOD_RESULT_OK) {
        return read;
    }
    const std::array<jlong, 1> fields{static_cast<jlong>(state)};
    return store_fields(*env, state_field, fields) ? MINEMOD_RESULT_OK : MINEMOD_RESULT_INVALID_ARGUMENT;
}

jint JNICALL read_frame(JNIEnv* env, jclass, jlong address, jobject pixels, jint offset, jint length) {
    const auto target = direct_range(*env, pixels, offset, length);
    if (!target) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return minemod_scene_session_read_frame(object_at<minemod_scene_session>(address), target->data(), target->size());
}

jint JNICALL read_counters(JNIEnv* env, jclass, jlong address, jlongArray fields) {
    minemod_scene_counters counters{};
    const minemod_result read =
        minemod_scene_session_get_counters(object_at<minemod_scene_session>(address), &counters);
    if (read != MINEMOD_RESULT_OK) {
        return read;
    }
    const std::array<jlong, 10> counter_fields{
        static_cast<jlong>(counters.frames_drawn),
        static_cast<jlong>(counters.frames_presented),
        static_cast<jlong>(counters.batches_drawn),
        static_cast<jlong>(counters.sections_published),
        static_cast<jlong>(counters.sections_dropped),
        static_cast<jlong>(counters.live_sections),
        static_cast<jlong>(counters.live_textures),
        static_cast<jlong>(counters.live_texture_bytes),
        static_cast<jlong>(counters.peak_upload_bytes),
        static_cast<jlong>(counters.peak_geometry_bytes),
    };
    return store_fields(*env, fields, counter_fields) ? MINEMOD_RESULT_OK : MINEMOD_RESULT_INVALID_ARGUMENT;
}

}

bool register_scene_session_calls(JNIEnv& env) noexcept {
    const std::array methods{
        native_method("nativeCreate", "(JIIIIJJJ[J)I", &create),
        native_method("nativeDestroy", "(J)V", &destroy),
        native_method("nativeSubmit", "(JLjava/nio/ByteBuffer;II[J)I", &submit),
        native_method("nativeResize", "(JII)I", &resize),
        native_method("nativeSetPacing", "(JI)I", &set_pacing),
        native_method("nativeReadPresentationState", "(J[J)I", &read_presentation_state),
        native_method("nativeReadFrame", "(JLjava/nio/ByteBuffer;II)I", &read_frame),
        native_method("nativeReadCounters", "(J[J)I", &read_counters),
    };
    return register_natives(env, "io/github/luasgraphicss/minemod/bridge/NativeSceneSession", methods);
}

}
