#include "shaders/shader_calls.hpp"

#include <array>
#include <cstddef>
#include <cstdint>
#include <optional>
#include <string>
#include <utility>
#include <vector>

#include "abi/abi_version_argument.hpp"
#include "arrays/java_arrays.hpp"
#include "arrays/native_text.hpp"
#include "fields/long_fields.hpp"
#include "library/native_registration.hpp"
#include "minemod/shaders/shader_service.h"
#include "scene/scene_addresses.hpp"

namespace minemod::jni {

namespace {

constexpr std::size_t max_path_bytes = MINEMOD_SHADER_MAX_PATH_BYTES - 1;

std::optional<std::vector<std::string>> copied_texts(JNIEnv& env, jobjectArray arrays, std::size_t max_bytes) {
    const jsize count = arrays == nullptr ? 0 : env.GetArrayLength(arrays);
    if (static_cast<std::size_t>(count) > MINEMOD_SHADER_MAX_INCLUDES) {
        return std::nullopt;
    }
    std::vector<std::string> copied;
    copied.reserve(static_cast<std::size_t>(count));
    for (jsize index = 0; index < count; ++index) {
        const auto element = static_cast<jbyteArray>(env.GetObjectArrayElement(arrays, index));
        std::optional<std::string> text = copied_bytes(env, element, max_bytes);
        env.DeleteLocalRef(element);
        if (!text) {
            return std::nullopt;
        }
        copied.push_back(std::move(*text));
    }
    return copied;
}

minemod_shader_text text_of(const std::string& text) noexcept {
    return minemod_shader_text{text.data(), text.size()};
}

jboolean JNICALL is_available(JNIEnv*, jclass) {
    return JNI_TRUE;
}

jint JNICALL create(
    JNIEnv* env,
    jclass,
    jint abi_major,
    jint abi_minor,
    jbyteArray compiler_path,
    jbyteArray scratch_directory,
    jint wall_time_milliseconds,
    jlong compiler_memory_bytes,
    jlong cache_bytes,
    jlongArray service_field) {
    const auto abi_version = abi_version_from(abi_major, abi_minor);
    if (!abi_version || wall_time_milliseconds < 0 || compiler_memory_bytes < 0 || cache_bytes < 0) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    minemod_shader_service* service = nullptr;
    try {
        const std::optional<std::string> compiler = copied_bytes(*env, compiler_path, max_path_bytes);
        const std::optional<std::string> scratch = copied_bytes(*env, scratch_directory, max_path_bytes);
        if (!compiler || !scratch) {
            return MINEMOD_RESULT_INVALID_ARGUMENT;
        }
        const minemod_shader_service_config config{
            .struct_bytes = sizeof(minemod_shader_service_config),
            .abi_version = *abi_version,
            .wall_time_milliseconds = static_cast<std::uint32_t>(wall_time_milliseconds),
            .reserved = 0,
            .compiler_memory_bytes = static_cast<std::uint64_t>(compiler_memory_bytes),
            .cache_bytes = static_cast<std::uint64_t>(cache_bytes),
            .compiler_path = compiler->c_str(),
            .scratch_directory = scratch->c_str(),
        };
        const minemod_result created = minemod_shader_service_create(&config, &service);
        if (created != MINEMOD_RESULT_OK) {
            return created;
        }
    } catch (...) {
        return MINEMOD_RESULT_OUT_OF_MEMORY;
    }
    const std::array<jlong, 1> address{address_of(service)};
    if (!store_fields(*env, service_field, address)) {
        minemod_shader_service_destroy(service);
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return MINEMOD_RESULT_OK;
}

void JNICALL destroy(JNIEnv*, jclass, jlong service) {
    minemod_shader_service_destroy(object_at<minemod_shader_service>(service));
}

jint JNICALL compile(
    JNIEnv* env,
    jclass,
    jlong service,
    jbyteArray name,
    jbyteArray vertex,
    jbyteArray fragment,
    jobjectArray include_keys,
    jobjectArray include_texts,
    jlongArray compilation_field) {
    minemod_shader_compilation* compilation = nullptr;
    try {
        const std::optional<std::string> shader_name = copied_bytes(*env, name, MINEMOD_SHADER_MAX_NAME_BYTES);
        const std::optional<std::string> vertex_text = copied_bytes(*env, vertex, MINEMOD_SHADER_MAX_SOURCE_BYTES);
        const std::optional<std::string> fragment_text =
            copied_bytes(*env, fragment, MINEMOD_SHADER_MAX_SOURCE_BYTES);
        const std::optional<std::vector<std::string>> keys =
            copied_texts(*env, include_keys, MINEMOD_SHADER_MAX_NAME_BYTES);
        const std::optional<std::vector<std::string>> texts =
            copied_texts(*env, include_texts, MINEMOD_SHADER_MAX_SOURCE_BYTES);
        if (!shader_name || !vertex_text || !fragment_text || !keys || !texts) {
            return MINEMOD_RESULT_LIMIT_EXCEEDED;
        }
        if (keys->size() != texts->size()) {
            return MINEMOD_RESULT_INVALID_ARGUMENT;
        }
        std::vector<minemod_shader_include> includes;
        includes.reserve(keys->size());
        for (std::size_t index = 0; index < keys->size(); ++index) {
            includes.push_back(minemod_shader_include{text_of((*keys)[index]), text_of((*texts)[index])});
        }
        const minemod_core_shader_source source{
            .struct_bytes = sizeof(minemod_core_shader_source),
            .include_count = static_cast<std::uint32_t>(includes.size()),
            .name = text_of(*shader_name),
            .vertex = text_of(*vertex_text),
            .fragment = text_of(*fragment_text),
            .includes = includes.data(),
        };
        const minemod_result compiled = minemod_shader_service_compile_core_shader(
            object_at<minemod_shader_service>(service), &source, &compilation);
        if (compiled != MINEMOD_RESULT_OK) {
            return compiled;
        }
    } catch (...) {
        return MINEMOD_RESULT_OUT_OF_MEMORY;
    }
    const std::array<jlong, 1> address{address_of(compilation)};
    if (!store_fields(*env, compilation_field, address)) {
        minemod_shader_compilation_destroy(compilation);
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    return MINEMOD_RESULT_OK;
}

void JNICALL destroy_compilation(JNIEnv*, jclass, jlong compilation) {
    minemod_shader_compilation_destroy(object_at<minemod_shader_compilation>(compilation));
}

jboolean JNICALL compilation_succeeded(JNIEnv*, jclass, jlong compilation) {
    const std::uint32_t succeeded =
        minemod_shader_compilation_succeeded(object_at<minemod_shader_compilation>(compilation));
    return succeeded != 0 ? JNI_TRUE : JNI_FALSE;
}

jintArray JNICALL compilation_words(JNIEnv* env, jclass, jlong compilation, jint stage) {
    if (stage < 0) {
        return nullptr;
    }
    const minemod_shader_compilation* compiled = object_at<minemod_shader_compilation>(compilation);
    const auto shader_stage = static_cast<std::uint32_t>(stage);
    try {
        std::vector<std::uint32_t> words(
            static_cast<std::size_t>(minemod_shader_compilation_read_words(compiled, shader_stage, nullptr, 0)));
        minemod_shader_compilation_read_words(compiled, shader_stage, words.data(), words.size());
        return new_words(*env, words);
    } catch (...) {
        return nullptr;
    }
}

jbyteArray JNICALL compilation_text(JNIEnv* env, jclass, jlong compilation) {
    const minemod_shader_compilation* compiled = object_at<minemod_shader_compilation>(compilation);
    return text_bytes(*env, [compiled](char* destination, std::uint64_t capacity) {
        return minemod_shader_compilation_read_text(compiled, destination, capacity);
    });
}

}

bool register_shader_calls(JNIEnv& env) noexcept {
    const std::array methods{
        native_method("nativeIsAvailable", "()Z", &is_available),
        native_method("nativeCreate", "(II[B[BIJJ[J)I", &create),
        native_method("nativeDestroy", "(J)V", &destroy),
        native_method("nativeCompile", "(J[B[B[B[[B[[B[J)I", &compile),
        native_method("nativeDestroyCompilation", "(J)V", &destroy_compilation),
        native_method("nativeCompilationSucceeded", "(J)Z", &compilation_succeeded),
        native_method("nativeCompilationWords", "(JI)[I", &compilation_words),
        native_method("nativeCompilationText", "(J)[B", &compilation_text),
    };
    return register_natives(env, "io/github/luasgraphicss/minemod/bridge/NativeShaderService", methods);
}

}
