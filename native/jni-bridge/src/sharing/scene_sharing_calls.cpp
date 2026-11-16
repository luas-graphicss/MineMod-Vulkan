#include "sharing/scene_sharing_calls.hpp"

#include <algorithm>
#include <array>
#include <cstddef>
#include <cstdint>

#include "arrays/java_arrays.hpp"
#include "fields/long_fields.hpp"
#include "library/native_registration.hpp"
#include "minemod/scene/scene_session.h"
#include "scene/scene_addresses.hpp"

namespace minemod::jni {

namespace {

constexpr std::size_t fields_per_attachment = 5;
constexpr std::size_t sharing_field_count = MINEMOD_SHARED_ATTACHMENT_COUNT * fields_per_attachment + 2;
constexpr std::size_t identity_bytes = 2 * MINEMOD_SCENE_DEVICE_UUID_BYTES;
constexpr std::size_t semaphore_count = 2;

std::array<jlong, sharing_field_count> sharing_fields(const minemod_scene_sharing& sharing) noexcept {
    std::array<jlong, sharing_field_count> fields{};
    std::size_t next = 0;
    for (const minemod_shared_attachment& attachment : sharing.attachments) {
        fields[next++] = static_cast<jlong>(attachment.allocation_bytes);
        fields[next++] = static_cast<jlong>(attachment.vulkan_format);
        fields[next++] = static_cast<jlong>(attachment.vulkan_layout);
        fields[next++] = static_cast<jlong>(attachment.width);
        fields[next++] = static_cast<jlong>(attachment.height);
    }
    fields[next++] = static_cast<jlong>(sharing.flags);
    fields[next] = static_cast<jlong>(sharing.generation);
    return fields;
}

std::array<std::uint8_t, identity_bytes> identity_of(const minemod_scene_sharing& sharing) noexcept {
    std::array<std::uint8_t, identity_bytes> identity{};
    std::ranges::copy(sharing.device_uuid, identity.begin());
    std::ranges::copy(sharing.driver_uuid, identity.begin() + MINEMOD_SCENE_DEVICE_UUID_BYTES);
    return identity;
}

jint JNICALL describe(JNIEnv* env, jclass, jlong session, jbyteArray identity, jlongArray fields) {
    minemod_scene_sharing sharing{};
    const minemod_result described =
        minemod_scene_session_get_sharing(object_at<minemod_scene_session>(session), &sharing);
    if (described != MINEMOD_RESULT_OK) {
        return described;
    }
    const bool is_stored =
        store_bytes(*env, identity, identity_of(sharing)) && store_fields(*env, fields, sharing_fields(sharing));
    return is_stored ? MINEMOD_RESULT_OK : MINEMOD_RESULT_INVALID_ARGUMENT;
}

jint JNICALL export_attachment(JNIEnv* env, jclass, jlong session, jint attachment, jlongArray handle_field) {
    if (attachment < 0 || !holds_fields(*env, handle_field, 1)) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    std::int64_t handle = -1;
    const minemod_result exported = minemod_scene_session_export_attachment(
        object_at<minemod_scene_session>(session), static_cast<std::uint32_t>(attachment), &handle);
    if (exported != MINEMOD_RESULT_OK) {
        return exported;
    }
    const std::array<jlong, 1> fields{static_cast<jlong>(handle)};
    return store_fields(*env, handle_field, fields) ? MINEMOD_RESULT_OK : MINEMOD_RESULT_INVALID_ARGUMENT;
}

jint JNICALL export_semaphores(JNIEnv* env, jclass, jlong session, jlongArray handle_fields) {
    if (!holds_fields(*env, handle_fields, semaphore_count)) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    std::int64_t renderer_done = -1;
    std::int64_t provider_done = -1;
    const minemod_result exported = minemod_scene_session_export_semaphores(
        object_at<minemod_scene_session>(session), &renderer_done, &provider_done);
    if (exported != MINEMOD_RESULT_OK) {
        return exported;
    }
    const std::array<jlong, semaphore_count> fields{
        static_cast<jlong>(renderer_done), static_cast<jlong>(provider_done)};
    return store_fields(*env, handle_fields, fields) ? MINEMOD_RESULT_OK : MINEMOD_RESULT_INVALID_ARGUMENT;
}

jint JNICALL release_to_provider(JNIEnv*, jclass, jlong session, jint synchronization) {
    if (synchronization < 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return minemod_scene_session_release_to_provider(
        object_at<minemod_scene_session>(session), static_cast<std::uint32_t>(synchronization));
}

jint JNICALL reclaim_for_renderer(JNIEnv*, jclass, jlong session) {
    return minemod_scene_session_reclaim_for_renderer(object_at<minemod_scene_session>(session));
}

}

bool register_scene_sharing_calls(JNIEnv& env) noexcept {
    const std::array methods{
        native_method("nativeDescribe", "(J[B[J)I", &describe),
        native_method("nativeExportAttachment", "(JI[J)I", &export_attachment),
        native_method("nativeExportSemaphores", "(J[J)I", &export_semaphores),
        native_method("nativeRelease", "(JI)I", &release_to_provider),
        native_method("nativeReclaim", "(J)I", &reclaim_for_renderer),
    };
    return register_natives(env, "io/github/luasgraphicss/minemod/bridge/SceneSharing", methods);
}

}
