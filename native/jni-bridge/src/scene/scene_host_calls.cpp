#include "scene/scene_host_calls.hpp"

#include <array>
#include <cstdint>
#include <optional>
#include <string>
#include <vector>

#include "abi/abi_version_argument.hpp"
#include "fields/long_fields.hpp"
#include "library/native_registration.hpp"
#include "minemod/scene/scene_session.h"
#include "scene/scene_addresses.hpp"

namespace minemod::jni {

namespace {

std::optional<std::string> text_of(JNIEnv& env, jstring text) {
    if (text == nullptr) {
        return std::nullopt;
    }
    const char* characters = env.GetStringUTFChars(text, nullptr);
    if (characters == nullptr) {
        return std::nullopt;
    }
    std::string copied(characters);
    env.ReleaseStringUTFChars(text, characters);
    return copied;
}

std::optional<std::vector<std::string>> texts_of(JNIEnv& env, jobjectArray texts) {
    std::vector<std::string> copied;
    const jsize count = texts == nullptr ? 0 : env.GetArrayLength(texts);
    if (static_cast<std::uint32_t>(count) > MINEMOD_SCENE_HOST_MAX_EXTENSIONS) {
        return std::nullopt;
    }
    for (jsize index = 0; index < count; ++index) {
        const auto element = static_cast<jstring>(env.GetObjectArrayElement(texts, index));
        std::optional<std::string> text = text_of(env, element);
        env.DeleteLocalRef(element);
        if (!text) {
            return std::nullopt;
        }
        copied.push_back(std::move(*text));
    }
    return copied;
}

minemod_result create_host(
    minemod_abi_version abi_version, bool validates, const std::vector<std::string>& extensions, minemod_scene_host** host) {
    std::vector<const char*> names;
    names.reserve(extensions.size());
    for (const std::string& extension : extensions) {
        names.push_back(extension.c_str());
    }
    const minemod_scene_host_config config{
        .struct_bytes = sizeof(minemod_scene_host_config),
        .abi_version = abi_version,
        .flags = validates ? MINEMOD_SCENE_HOST_VALIDATION : 0,
        .extension_count = static_cast<std::uint32_t>(names.size()),
        .extensions = names.data(),
    };
    return minemod_scene_host_create(&config, host);
}

jint JNICALL create(
    JNIEnv* env,
    jclass,
    jint abi_major,
    jint abi_minor,
    jboolean validates,
    jobjectArray extension_names,
    jlongArray host_field) {
    const auto abi_version = abi_version_from(abi_major, abi_minor);
    if (!abi_version) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    minemod_scene_host* host = nullptr;
    try {
        const std::optional<std::vector<std::string>> extensions = texts_of(*env, extension_names);
        if (!extensions) {
            return MINEMOD_RESULT_INVALID_ARGUMENT;
        }
        const minemod_result created = create_host(*abi_version, validates == JNI_TRUE, *extensions, &host);
        if (created != MINEMOD_RESULT_OK) {
            return created;
        }
    } catch (...) {
        return MINEMOD_RESULT_OUT_OF_MEMORY;
    }
    const std::array<jlong, 1> address{address_of(host)};
    if (!store_fields(*env, host_field, address)) {
        minemod_scene_host_destroy(host);
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return MINEMOD_RESULT_OK;
}

void JNICALL destroy(JNIEnv*, jclass, jlong address) {
    minemod_scene_host_destroy(object_at<minemod_scene_host>(address));
}

jlong JNICALL vulkan_instance(JNIEnv*, jclass, jlong address) {
    return static_cast<jlong>(minemod_scene_host_vulkan_instance(object_at<minemod_scene_host>(address)));
}

jstring JNICALL validation_text(JNIEnv* env, jclass, jlong address) {
    const minemod_scene_host* host = object_at<minemod_scene_host>(address);
    try {
        std::string text(minemod_scene_host_read_validation_text(host, nullptr, 0), '\0');
        minemod_scene_host_read_validation_text(host, text.data(), text.size());
        return env->NewStringUTF(text.c_str());
    } catch (...) {
        return nullptr;
    }
}

}

bool register_scene_host_calls(JNIEnv& env) noexcept {
    const std::array methods{
        native_method("nativeCreate", "(IIZ[Ljava/lang/String;[J)I", &create),
        native_method("nativeDestroy", "(J)V", &destroy),
        native_method("nativeVulkanInstance", "(J)J", &vulkan_instance),
        native_method("nativeValidationText", "(J)Ljava/lang/String;", &validation_text),
    };
    return register_natives(env, "io/github/luasgraphicss/minemod/bridge/NativeSceneHost", methods);
}

}
