#include <bit>
#include <cstdint>
#include <initializer_list>

#include "fake_provider/fake_calls.hpp"
#include "fake_provider/fake_export.hpp"

extern "C" int fake_dependency_answer();

namespace {

FakeCall last_call{};
int mapped_storage = 0;

void record(const char* function, std::initializer_list<std::int64_t> words) {
    last_call.function = function;
    last_call.word_count = words.size();
    std::size_t index = 0;
    for (const std::int64_t word : words) {
        last_call.words[index++] = word;
    }
}

std::int64_t bits(float value) {
    return static_cast<std::int64_t>(std::bit_cast<std::uint32_t>(value));
}

std::int64_t bits(double value) {
    return std::bit_cast<std::int64_t>(value);
}

std::int64_t bits(const void* pointer) {
    return static_cast<std::int64_t>(reinterpret_cast<std::uintptr_t>(pointer));
}

}

FAKE_EXPORT const FakeCall* fake_provider_last_call() {
    return &last_call;
}

FAKE_EXPORT void glClearColor(float red, float green, float blue, float alpha) {
    record("glClearColor", {bits(red), bits(green), bits(blue), bits(alpha)});
}

FAKE_EXPORT void glVertex3d(double x, double y, double z) {
    record("glVertex3d", {bits(x), bits(y), bits(z)});
}

FAKE_EXPORT void glBindTexture(std::uint32_t target, std::uint32_t texture) {
    record("glBindTexture", {target, texture});
}

FAKE_EXPORT void glGenTextures(std::int32_t count, std::uint32_t* textures) {
    record("glGenTextures", {count, bits(textures)});
    for (std::int32_t index = 0; index < count; ++index) {
        textures[index] = static_cast<std::uint32_t>(fake_dependency_answer() + index);
    }
}

FAKE_EXPORT void glTexImage2D(
    std::uint32_t target,
    std::int32_t level,
    std::int32_t internal_format,
    std::int32_t width,
    std::int32_t height,
    std::int32_t border,
    std::uint32_t format,
    std::uint32_t type,
    const void* pixels) {
    record("glTexImage2D", {target, level, internal_format, width, height, border, format, type, bits(pixels)});
}

FAKE_EXPORT std::uint8_t glIsTexture(std::uint32_t texture) {
    record("glIsTexture", {texture});
    return texture == static_cast<std::uint32_t>(fake_dependency_answer()) ? 1 : 0;
}

FAKE_EXPORT void* glMapBuffer(std::uint32_t target, std::uint32_t access) {
    record("glMapBuffer", {target, access});
    return &mapped_storage;
}

FAKE_EXPORT std::uint32_t glGetError() {
    record("glGetError", {});
    return 0x0502;
}
