#include "memory/memory_natives.hpp"

#include <array>
#include <cstdint>
#include <cstdlib>
#include <cstring>

#include "library/binding_class.hpp"
#include "library/native_registration.hpp"

namespace minemod::provider::jni {

namespace {

template <typename Value>
Value read(jlong address) noexcept {
    Value value{};
    std::memcpy(&value, reinterpret_cast<const void*>(static_cast<std::uintptr_t>(address)), sizeof(Value));
    return value;
}

template <typename Value>
void write(jlong address, Value value) noexcept {
    std::memcpy(reinterpret_cast<void*>(static_cast<std::uintptr_t>(address)), &value, sizeof(Value));
}

jlong allocate(JNIEnv*, jclass, jlong bytes) noexcept {
    if (bytes <= 0) {
        return 0;
    }
    return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(std::calloc(1, static_cast<std::size_t>(bytes))));
}

void release(JNIEnv*, jclass, jlong address) noexcept {
    std::free(reinterpret_cast<void*>(static_cast<std::uintptr_t>(address)));
}

jint read_int(JNIEnv*, jclass, jlong address) noexcept {
    return read<jint>(address);
}

void write_int(JNIEnv*, jclass, jlong address, jint value) noexcept {
    write(address, value);
}

jlong read_long(JNIEnv*, jclass, jlong address) noexcept {
    return read<jlong>(address);
}

void write_long(JNIEnv*, jclass, jlong address, jlong value) noexcept {
    write(address, value);
}

jbyte read_byte(JNIEnv*, jclass, jlong address) noexcept {
    return read<jbyte>(address);
}

void write_byte(JNIEnv*, jclass, jlong address, jbyte value) noexcept {
    write(address, value);
}

void write_bytes(JNIEnv* env, jclass, jlong address, jbyteArray source, jint offset, jint count) noexcept {
    env->GetByteArrayRegion(source, offset, count, reinterpret_cast<jbyte*>(static_cast<std::uintptr_t>(address)));
}

}

bool register_memory_natives(JNIEnv& env) noexcept {
    const std::array methods{
        minemod::jni::native_method("allocate", "(J)J", &allocate),
        minemod::jni::native_method("free", "(J)V", &release),
        minemod::jni::native_method("readInt", "(J)I", &read_int),
        minemod::jni::native_method("writeInt", "(JI)V", &write_int),
        minemod::jni::native_method("readLong", "(J)J", &read_long),
        minemod::jni::native_method("writeLong", "(JJ)V", &write_long),
        minemod::jni::native_method("readByte", "(J)B", &read_byte),
        minemod::jni::native_method("writeByte", "(JB)V", &write_byte),
        minemod::jni::native_method("writeBytes", "(J[BII)V", &write_bytes),
    };
    return minemod::jni::register_natives(env, binding_class, methods);
}

}
