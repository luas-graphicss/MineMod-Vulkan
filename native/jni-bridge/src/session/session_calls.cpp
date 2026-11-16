#include "session/session_calls.hpp"

#include <array>
#include <cstdint>

#include "abi/abi_version_argument.hpp"
#include "buffers/direct_buffer.hpp"
#include "fields/long_fields.hpp"
#include "library/native_registration.hpp"
#include "minemod/abi/session.h"
#include "stream/stream_report_fields.hpp"

namespace minemod::jni {

namespace {

minemod_session* session_at(jlong address) noexcept {
    return reinterpret_cast<minemod_session*>(static_cast<std::intptr_t>(address));
}

jlong address_of(minemod_session* session) noexcept {
    return static_cast<jlong>(reinterpret_cast<std::intptr_t>(session));
}

jint JNICALL create_session(
    JNIEnv* env,
    jclass,
    jint abi_major,
    jint abi_minor,
    jlong required_capabilities,
    jint max_buffers,
    jlong max_buffer_bytes,
    jlongArray session_field) {
    const auto abi_version = abi_version_from(abi_major, abi_minor);
    if (!abi_version || max_buffers < 0 || max_buffer_bytes < 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const minemod_session_config config{
        .struct_bytes = sizeof(minemod_session_config),
        .abi_version = *abi_version,
        .required_capabilities = static_cast<minemod_capabilities>(required_capabilities),
        .max_buffers = static_cast<std::uint32_t>(max_buffers),
        .reserved = 0,
        .max_buffer_bytes = static_cast<std::uint64_t>(max_buffer_bytes),
    };
    minemod_session* session = nullptr;
    if (const minemod_result created = minemod_session_create(&config, &session); created != MINEMOD_RESULT_OK) {
        return created;
    }

    const std::array<jlong, 1> address{address_of(session)};
    if (!store_fields(*env, session_field, address)) {
        minemod_session_destroy(session);
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return MINEMOD_RESULT_OK;
}

void JNICALL destroy_session(JNIEnv*, jclass, jlong address) {
    minemod_session_destroy(session_at(address));
}

jint JNICALL submit(
    JNIEnv* env, jclass, jlong address, jobject stream, jint offset, jint length, jlongArray fields) {
    minemod_stream_report report = rejected_stream_report();
    minemod_result result = MINEMOD_RESULT_INVALID_ARGUMENT;
    if (const auto bytes = direct_range(*env, stream, offset, length)) {
        result = minemod_session_submit(session_at(address), bytes->data(), bytes->size(), &report);
    }
    return store_fields(*env, fields, stream_report_fields(report)) ? result : MINEMOD_RESULT_INVALID_ARGUMENT;
}

jint JNICALL read_buffer(
    JNIEnv* env,
    jclass,
    jlong address,
    jlong buffer,
    jlong buffer_offset,
    jobject destination,
    jint destination_offset,
    jint length) {
    const auto target = direct_range(*env, destination, destination_offset, length);
    if (!target || buffer_offset < 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return minemod_session_read_buffer(
        session_at(address),
        static_cast<minemod_handle>(buffer),
        static_cast<std::uint64_t>(buffer_offset),
        target->data(),
        target->size());
}

jint JNICALL read_counters(JNIEnv* env, jclass, jlong address, jlongArray fields) {
    minemod_session_counters counters{};
    if (const minemod_result read = minemod_session_get_counters(session_at(address), &counters);
        read != MINEMOD_RESULT_OK) {
        return read;
    }
    const std::array<jlong, 8> counter_fields{
        static_cast<jlong>(counters.submissions_accepted),
        static_cast<jlong>(counters.submissions_rejected),
        static_cast<jlong>(counters.submissions_aborted),
        static_cast<jlong>(counters.commands_executed),
        static_cast<jlong>(counters.buffers_created),
        static_cast<jlong>(counters.buffers_destroyed),
        static_cast<jlong>(counters.live_buffers),
        static_cast<jlong>(counters.live_buffer_bytes),
    };
    return store_fields(*env, fields, counter_fields) ? MINEMOD_RESULT_OK : MINEMOD_RESULT_INVALID_ARGUMENT;
}

}

bool register_session_calls(JNIEnv& env) noexcept {
    const std::array methods{
        native_method("nativeCreate", "(IIJIJ[J)I", &create_session),
        native_method("nativeDestroy", "(J)V", &destroy_session),
        native_method("nativeSubmit", "(JLjava/nio/ByteBuffer;II[J)I", &submit),
        native_method("nativeReadBuffer", "(JJJLjava/nio/ByteBuffer;II)I", &read_buffer),
        native_method("nativeReadCounters", "(J[J)I", &read_counters),
    };
    return register_natives(env, "io/github/luasgraphicss/minemod/bridge/NativeSession", methods);
}

}
