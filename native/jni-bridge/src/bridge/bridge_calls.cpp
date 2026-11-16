#include "bridge/bridge_calls.hpp"

#include <array>

#include "abi/abi_version_argument.hpp"
#include "buffers/direct_buffer.hpp"
#include "fields/long_fields.hpp"
#include "library/native_registration.hpp"
#include "minemod/abi/command_stream.h"
#include "minemod/abi/negotiation.h"
#include "scene/scene_calls.hpp"
#include "stream/stream_report_fields.hpp"

namespace minemod::jni {

namespace {

jint JNICALL negotiate(
    JNIEnv* env, jclass, jint client_major, jint client_minor, jlong required_capabilities, jlongArray fields) {
    const auto client_version = abi_version_from(client_major, client_minor);
    if (!client_version) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const minemod_negotiation_request request{
        .struct_bytes = sizeof(minemod_negotiation_request),
        .client_version = *client_version,
        .required_capabilities = static_cast<minemod_capabilities>(required_capabilities),
    };
    minemod_negotiation negotiation{};
    const minemod_result result = minemod_negotiate(&request, &negotiation);

    const std::array<jlong, 6> negotiation_fields{
        jlong{negotiation.library_version.major},
        jlong{negotiation.library_version.minor},
        static_cast<jlong>(negotiation.supported_capabilities),
        static_cast<jlong>(negotiation.missing_capabilities),
        jlong{negotiation.max_stream_bytes},
        jlong{negotiation.max_stream_commands},
    };
    return store_fields(*env, fields, negotiation_fields) ? result : MINEMOD_RESULT_INVALID_ARGUMENT;
}

jint JNICALL validate_stream(JNIEnv* env, jclass, jobject stream, jint offset, jint length, jlongArray fields) {
    minemod_stream_report report = rejected_stream_report();
    minemod_result result = MINEMOD_RESULT_INVALID_ARGUMENT;
    if (const auto bytes = direct_range(*env, stream, offset, length)) {
        result = minemod_validate_command_stream(bytes->data(), bytes->size(), &report);
    }
    return store_fields(*env, fields, stream_report_fields(report)) ? result : MINEMOD_RESULT_INVALID_ARGUMENT;
}

jboolean JNICALL library_draws_scenes(JNIEnv*, jclass) {
    return draws_scenes() ? JNI_TRUE : JNI_FALSE;
}

}

bool register_bridge_calls(JNIEnv& env) noexcept {
    const std::array methods{
        native_method("nativeNegotiate", "(IIJ[J)I", &negotiate),
        native_method("nativeValidateStream", "(Ljava/nio/ByteBuffer;II[J)I", &validate_stream),
        native_method("nativeDrawsScenes", "()Z", &library_draws_scenes),
    };
    return register_natives(env, "io/github/luasgraphicss/minemod/bridge/NativeBridge", methods);
}

}
