#include "calls/call_natives.hpp"

#include <array>
#include <cstdint>
#include <string>

#include "calls/call_gate.hpp"
#include "calls/call_table.hpp"
#include "errors/java_exception.hpp"
#include "library/binding_class.hpp"
#include "library/native_registration.hpp"
#include "provider/session_handle.hpp"

namespace minemod::provider::jni {

namespace {

jobjectArray call_names(JNIEnv* env, jclass) noexcept {
    const std::span<const CallRecord> records = call_records();
    const jclass string_class = env->FindClass("java/lang/String");
    if (string_class == nullptr) {
        return nullptr;
    }
    const jobjectArray names = env->NewObjectArray(static_cast<jsize>(records.size()), string_class, nullptr);
    for (std::size_t index = 0; names != nullptr && index < records.size(); ++index) {
        const jstring name = env->NewStringUTF(std::string(records[index].name).c_str());
        if (name == nullptr) {
            return nullptr;
        }
        env->SetObjectArrayElement(names, static_cast<jsize>(index), name);
        env->DeleteLocalRef(name);
    }
    return names;
}

jlong thunk_address_of(JNIEnv*, jclass, jint function) noexcept {
    const std::span<const CallRecord> records = call_records();
    if (function < 0 || static_cast<std::size_t>(function) >= records.size()) {
        return 0;
    }
    return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(records[static_cast<std::size_t>(function)].thunk));
}

jlong invoke(JNIEnv* env, jclass, jlong provider, jint function, jlongArray packed) noexcept {
    return guarded(*env, jlong{0}, [&] {
        const jsize count = env->GetArrayLength(packed);
        if (function < 0 || static_cast<std::size_t>(count) > max_call_words) {
            throw CallError("the invocation names no function or carries too many arguments");
        }
        std::array<jlong, max_call_words> words{};
        env->GetLongArrayRegion(packed, 0, count, words.data());
        const std::span<const std::int64_t> carried(
            reinterpret_cast<const std::int64_t*>(words.data()), static_cast<std::size_t>(count));
        return static_cast<jlong>(session_of(provider).invoke(static_cast<std::uint32_t>(function), carried));
    });
}

jlong unanswered_calls(JNIEnv*, jclass) noexcept {
    return static_cast<jlong>(unanswered_call_count());
}

}

bool register_call_natives(JNIEnv& env) noexcept {
    const std::array methods{
        minemod::jni::native_method("callNames", "()[Ljava/lang/String;", &call_names),
        minemod::jni::native_method("thunkAddress", "(I)J", &thunk_address_of),
        minemod::jni::native_method("invoke", "(JI[J)J", &invoke),
        minemod::jni::native_method("unansweredCallCount", "()J", &unanswered_calls),
    };
    return minemod::jni::register_natives(env, binding_class, methods);
}

}
