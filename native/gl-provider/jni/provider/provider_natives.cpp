#include "provider/provider_natives.hpp"

#include <array>
#include <filesystem>
#include <memory>
#include <string>
#include <vector>

#include "errors/java_exception.hpp"
#include "library/binding_class.hpp"
#include "library/driver_settings.hpp"
#include "library/native_registration.hpp"
#include "provider/session_handle.hpp"

namespace minemod::provider::jni {

namespace {

std::string text_of(JNIEnv& env, jstring text) {
    const char* characters = env.GetStringUTFChars(text, nullptr);
    if (characters == nullptr) {
        throw std::bad_alloc();
    }
    std::string copied(characters);
    env.ReleaseStringUTFChars(text, characters);
    return copied;
}

std::filesystem::path path_of(JNIEnv& env, jstring text) {
    const jsize length = env.GetStringLength(text);
    std::vector<jchar> units(static_cast<std::size_t>(length));
    env.GetStringRegion(text, 0, length, units.data());
    return std::filesystem::path(std::u16string(units.begin(), units.end()));
}

std::vector<std::string> texts_of(JNIEnv& env, jobjectArray texts) {
    const jsize count = env.GetArrayLength(texts);
    std::vector<std::string> copied;
    copied.reserve(static_cast<std::size_t>(count));
    for (jsize index = 0; index < count; ++index) {
        const auto text = static_cast<jstring>(env.GetObjectArrayElement(texts, index));
        copied.push_back(text_of(env, text));
        env.DeleteLocalRef(text);
    }
    return copied;
}

EglHandle context_of(jlong handle) noexcept {
    return reinterpret_cast<EglHandle>(static_cast<std::uintptr_t>(handle));
}

jlong handle_of(const void* pointer) noexcept {
    return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(pointer));
}

std::vector<DriverSetting> settings_of(JNIEnv& env, jobjectArray names, jobjectArray values) {
    const std::vector<std::string> setting_names = texts_of(env, names);
    const std::vector<std::string> setting_values = texts_of(env, values);
    if (setting_names.size() != setting_values.size()) {
        throw LibraryLoadError("every driver setting has one value");
    }
    std::vector<DriverSetting> settings;
    settings.reserve(setting_names.size());
    for (std::size_t index = 0; index < setting_names.size(); ++index) {
        settings.push_back(DriverSetting{setting_names[index], setting_values[index]});
    }
    return settings;
}

jlong open_provider(
    JNIEnv* env,
    jclass,
    jstring directory,
    jobjectArray dependencies,
    jstring entry,
    jobjectArray setting_names,
    jobjectArray setting_values) noexcept {
    return guarded(*env, jlong{0}, [&] {
        apply_driver_settings(settings_of(*env, setting_names, setting_values));
        const std::vector<std::string> preloaded = texts_of(*env, dependencies);
        auto session = std::make_unique<ProviderSession>(path_of(*env, directory), preloaded, text_of(*env, entry));
        return handle_of(session.release());
    });
}

void close_provider(JNIEnv*, jclass, jlong provider) noexcept {
    delete reinterpret_cast<ProviderSession*>(static_cast<std::uintptr_t>(provider));
}

jlongArray image_segments(JNIEnv* env, jclass, jlong provider) noexcept {
    return guarded(*env, static_cast<jlongArray>(nullptr), [&] {
        const std::span<const ImageSegment> image = session_of(provider).image();
        std::vector<jlong> bounds;
        bounds.reserve(image.size() * 2);
        for (const ImageSegment& segment : image) {
            bounds.push_back(static_cast<jlong>(segment.begin));
            bounds.push_back(static_cast<jlong>(segment.end));
        }
        const jlongArray packed = env->NewLongArray(static_cast<jsize>(bounds.size()));
        if (packed != nullptr) {
            env->SetLongArrayRegion(packed, 0, static_cast<jsize>(bounds.size()), bounds.data());
        }
        return packed;
    });
}

jlong function_address(JNIEnv* env, jclass, jlong provider, jstring function) noexcept {
    return guarded(*env, jlong{0}, [&] {
        return handle_of(session_of(provider).function_address(text_of(*env, function).c_str()));
    });
}

jlong create_context(
    JNIEnv* env, jclass, jlong provider, jboolean core_profile, jint major, jint minor, jlong shared_with) noexcept {
    return guarded(*env, jlong{0}, [&] {
        const ContextRequest request{
            core_profile == JNI_TRUE ? ContextProfile::core : ContextProfile::compatibility, major, minor};
        return handle_of(session_of(provider).display().create_context(request, context_of(shared_with)));
    });
}

void destroy_context(JNIEnv* env, jclass, jlong provider, jlong context) noexcept {
    guarded(*env, [&] {
        session_of(provider).display().destroy_context(context_of(context));
    });
}

void make_current(JNIEnv* env, jclass, jlong provider, jlong context) noexcept {
    guarded(*env, [&] {
        session_of(provider).display().make_current(context_of(context));
    });
}

}

bool register_provider_natives(JNIEnv& env) noexcept {
    const std::array methods{
        minemod::jni::native_method(
            "openProvider",
            "(Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;[Ljava/lang/String;[Ljava/lang/String;)J",
            &open_provider),
        minemod::jni::native_method("closeProvider", "(J)V", &close_provider),
        minemod::jni::native_method("imageSegments", "(J)[J", &image_segments),
        minemod::jni::native_method("functionAddress", "(JLjava/lang/String;)J", &function_address),
        minemod::jni::native_method("createContext", "(JZIIJ)J", &create_context),
        minemod::jni::native_method("destroyContext", "(JJ)V", &destroy_context),
        minemod::jni::native_method("makeCurrent", "(JJ)V", &make_current),
    };
    return minemod::jni::register_natives(env, binding_class, methods);
}

}
