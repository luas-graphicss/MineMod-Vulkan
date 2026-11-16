#include "buffers/direct_buffer.hpp"

#include <cstddef>

namespace minemod::jni {

std::optional<std::span<std::uint8_t>> direct_range(JNIEnv& env, jobject buffer, jint offset, jint length) noexcept {
    if (buffer == nullptr || offset < 0 || length < 0) {
        return std::nullopt;
    }
    auto* const address = static_cast<std::uint8_t*>(env.GetDirectBufferAddress(buffer));
    const jlong capacity = env.GetDirectBufferCapacity(buffer);
    if (address == nullptr || jlong{offset} + jlong{length} > capacity) {
        return std::nullopt;
    }
    return std::span<std::uint8_t>(address + offset, static_cast<std::size_t>(length));
}

}
