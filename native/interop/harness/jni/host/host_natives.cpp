#include "host/host_natives.hpp"

#include <array>
#include <cstdint>
#include <memory>
#include <stdexcept>
#include <string>

#include "errors/java_failure.hpp"
#include "harness/shared_scene_host.hpp"
#include "library/native_registration.hpp"
#include "memory/exported_handle.hpp"

namespace minemod::interop::harness::jni {

namespace {

constexpr const char* natives_class =
    "io/github/luasgraphicss/minemod/compatibility/runtime/fixtures/renderer/StandInNatives";
constexpr float channel_maximum = 255.0F;
constexpr std::uint32_t channel_mask = 0xFF;
constexpr int red_shift = 24;
constexpr int green_shift = 16;
constexpr int blue_shift = 8;

SharedSceneHost& host_of(jlong handle) {
    if (handle == 0) {
        throw std::invalid_argument("the stand-in renderer was closed");
    }
    return *reinterpret_cast<SharedSceneHost*>(static_cast<std::uintptr_t>(handle));
}

std::uint32_t count_of(jint value, const char* meaning) {
    if (value < 1) {
        throw std::invalid_argument(std::string(meaning) + " must be positive");
    }
    return static_cast<std::uint32_t>(value);
}

std::uint32_t index_of(jint value, const char* meaning) {
    if (value < 0) {
        throw std::invalid_argument(std::string(meaning) + " must not be negative");
    }
    return static_cast<std::uint32_t>(value);
}

const SceneImage& attachment_of(jlong host, jint attachment) {
    return host_of(host).image(index_of(attachment, "an attachment"));
}

SceneImageKind kind_of(jboolean color) noexcept {
    return color == JNI_TRUE ? SceneImageKind::color : SceneImageKind::depth_stencil;
}

float channel_of(jint rgba, int shift) noexcept {
    return static_cast<float>((static_cast<std::uint32_t>(rgba) >> shift) & channel_mask) / channel_maximum;
}

jlong open_host(JNIEnv* env, jclass) noexcept {
    return guarded(*env, jlong{0}, [] {
        return static_cast<jlong>(reinterpret_cast<std::uintptr_t>(std::make_unique<SharedSceneHost>().release()));
    });
}

void close_host(JNIEnv*, jclass, jlong host) noexcept {
    const std::unique_ptr<SharedSceneHost> closing(
        reinterpret_cast<SharedSceneHost*>(static_cast<std::uintptr_t>(host)));
}

jbyteArray device_identity(JNIEnv* env, jclass, jlong host) noexcept {
    return guarded(*env, static_cast<jbyteArray>(nullptr), [&] {
        const DeviceIdentity identity = host_of(host).identity();
        const jbyteArray uuids = env->NewByteArray(2 * VK_UUID_SIZE);
        if (uuids != nullptr) {
            env->SetByteArrayRegion(
                uuids, 0, VK_UUID_SIZE, reinterpret_cast<const jbyte*>(identity.device_uuid.data()));
            env->SetByteArrayRegion(
                uuids, VK_UUID_SIZE, VK_UUID_SIZE, reinterpret_cast<const jbyte*>(identity.driver_uuid.data()));
        }
        return uuids;
    });
}

jlongArray attachment_support(JNIEnv* env, jclass, jlong host, jboolean color, jint format) noexcept {
    return guarded(*env, static_cast<jlongArray>(nullptr), [&] {
        const ExternalSupport support = host_of(host).support_for(kind_of(color), static_cast<VkFormat>(format));
        const std::array<jlong, 4> answer{
            support.exportable ? 1 : 0,
            static_cast<jlong>(support.max_extent.width),
            static_cast<jlong>(support.max_extent.height),
            static_cast<jlong>(support.max_mip_levels),
        };
        const jlongArray packed = env->NewLongArray(static_cast<jsize>(answer.size()));
        if (packed != nullptr) {
            env->SetLongArrayRegion(packed, 0, static_cast<jsize>(answer.size()), answer.data());
        }
        return packed;
    });
}

jboolean semaphores_shareable(JNIEnv* env, jclass, jlong host) noexcept {
    return guarded(*env, jboolean{JNI_FALSE}, [&] {
        return host_of(host).can_share_semaphores() ? jboolean{JNI_TRUE} : jboolean{JNI_FALSE};
    });
}

jint create_attachment(
    JNIEnv* env, jclass, jlong host, jboolean color, jint format, jint width, jint height) noexcept {
    return guarded(*env, jint{0}, [&] {
        const SceneImageDescription description{
            kind_of(color),
            {count_of(width, "the width of an attachment"), count_of(height, "the height of an attachment")},
            static_cast<VkFormat>(format),
        };
        return static_cast<jint>(host_of(host).create_image(description));
    });
}

void destroy_attachment(JNIEnv* env, jclass, jlong host, jint attachment) noexcept {
    guarded(*env, [&] {
        host_of(host).destroy_image(index_of(attachment, "an attachment"));
    });
}

jlong allocation_size(JNIEnv* env, jclass, jlong host, jint attachment) noexcept {
    return guarded(*env, jlong{0}, [&] {
        return static_cast<jlong>(attachment_of(host, attachment).shared().allocation_size());
    });
}

jint export_memory(JNIEnv* env, jclass, jlong host, jint attachment) noexcept {
    return guarded(*env, jint{-1}, [&] {
        return static_cast<jint>(attachment_of(host, attachment).shared().export_memory().release());
    });
}

void clear_color(
    JNIEnv* env, jclass, jlong host, jint attachment, jint x, jint y, jint width, jint height, jint rgba) noexcept {
    guarded(*env, [&] {
        const VkRect2D area{
            {static_cast<std::int32_t>(index_of(x, "the left edge")), static_cast<std::int32_t>(index_of(y, "the top edge"))},
            {count_of(width, "the width of a pass"), count_of(height, "the height of a pass")},
        };
        const VkClearValue color{.color = {.float32 = {
            channel_of(rgba, red_shift), channel_of(rgba, green_shift), channel_of(rgba, blue_shift), channel_of(rgba, 0),
        }}};
        attachment_of(host, attachment).clear(area, color);
    });
}

void clear_depth(JNIEnv* env, jclass, jlong host, jint attachment, jint width, jint height, jfloat depth) noexcept {
    guarded(*env, [&] {
        const VkRect2D area{
            {0, 0}, {count_of(width, "the width of a pass"), count_of(height, "the height of a pass")}};
        attachment_of(host, attachment).clear(area, VkClearValue{.depthStencil = {depth, 0}});
    });
}

jint color_at(JNIEnv* env, jclass, jlong host, jint attachment, jint x, jint y) noexcept {
    return guarded(*env, jint{0}, [&] {
        const renderer::Pixel texel = attachment_of(host, attachment)
            .read_color()
            .at(index_of(x, "the column of a texel"), index_of(y, "the row of a texel"));
        const std::uint32_t packed = (std::uint32_t{texel.red} << red_shift) | (std::uint32_t{texel.green} << green_shift)
            | (std::uint32_t{texel.blue} << blue_shift) | std::uint32_t{texel.alpha};
        return static_cast<jint>(packed);
    });
}

jstring validation_text(JNIEnv* env, jclass, jlong host) noexcept {
    return guarded(*env, static_cast<jstring>(nullptr), [&] {
        return env->NewStringUTF(host_of(host).validation_text().c_str());
    });
}

void close_descriptor(JNIEnv*, jclass, jint descriptor) noexcept {
    const ExportedHandle closing(descriptor);
}

}

bool register_host_natives(JNIEnv& env) noexcept {
    const std::array methods{
        minemod::jni::native_method("open", "()J", &open_host),
        minemod::jni::native_method("close", "(J)V", &close_host),
        minemod::jni::native_method("deviceIdentity", "(J)[B", &device_identity),
        minemod::jni::native_method("attachmentSupport", "(JZI)[J", &attachment_support),
        minemod::jni::native_method("semaphoresShareable", "(J)Z", &semaphores_shareable),
        minemod::jni::native_method("createAttachment", "(JZIII)I", &create_attachment),
        minemod::jni::native_method("destroyAttachment", "(JI)V", &destroy_attachment),
        minemod::jni::native_method("allocationSize", "(JI)J", &allocation_size),
        minemod::jni::native_method("exportMemory", "(JI)I", &export_memory),
        minemod::jni::native_method("clearColor", "(JIIIIII)V", &clear_color),
        minemod::jni::native_method("clearDepth", "(JIIIF)V", &clear_depth),
        minemod::jni::native_method("colorAt", "(JIII)I", &color_at),
        minemod::jni::native_method("validationText", "(J)Ljava/lang/String;", &validation_text),
        minemod::jni::native_method("closeDescriptor", "(I)V", &close_descriptor),
    };
    return minemod::jni::register_natives(env, natives_class, methods);
}

}
