#include "window/awt_surface_calls.hpp"

#include <array>
#include <bit>

#include <jawt.h>

#include "fields/long_fields.hpp"
#include "library/native_registration.hpp"
#include "minemod/abi/result.h"
#include "scene/scene_addresses.hpp"
#include "window/awt_native_interface.hpp"
#include "window/drawing_surface.hpp"

namespace minemod::jni {

namespace {

class DrawingSurfaceLock {
public:
    DrawingSurfaceLock(const JAWT& awt, JNIEnv& env, jobject canvas) noexcept : awt_(awt) {
        surface_ = awt_.GetDrawingSurface(&env, canvas);
        if (surface_ == nullptr) {
            return;
        }
        if ((surface_->Lock(surface_) & JAWT_LOCK_ERROR) != 0) {
            awt_.FreeDrawingSurface(surface_);
            surface_ = nullptr;
            return;
        }
        info_ = surface_->GetDrawingSurfaceInfo(surface_);
    }

    DrawingSurfaceLock(const DrawingSurfaceLock&) = delete;
    DrawingSurfaceLock& operator=(const DrawingSurfaceLock&) = delete;

    ~DrawingSurfaceLock() {
        if (surface_ == nullptr) {
            return;
        }
        if (info_ != nullptr) {
            surface_->FreeDrawingSurfaceInfo(info_);
        }
        surface_->Unlock(surface_);
        awt_.FreeDrawingSurface(surface_);
    }

    const JAWT_DrawingSurfaceInfo* info() const noexcept {
        return info_;
    }

private:
    const JAWT& awt_;
    JAWT_DrawingSurface* surface_ = nullptr;
    JAWT_DrawingSurfaceInfo* info_ = nullptr;
};

jint JNICALL create(JNIEnv* env, jclass, jlong instance, jobject canvas, jlongArray surface_field) {
    if (instance == 0 || canvas == nullptr || !holds_fields(*env, surface_field, 1)) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }
    const awt_entry_point get_awt = find_awt_entry_point();
    JAWT awt{};
    awt.version = JAWT_VERSION_1_4;
    if (get_awt == nullptr || get_awt(env, &awt) == JNI_FALSE) {
        return MINEMOD_RESULT_UNSUPPORTED_CAPABILITY;
    }
    VkSurfaceKHR surface = VK_NULL_HANDLE;
    {
        const DrawingSurfaceLock locked(awt, *env, canvas);
        if (locked.info() == nullptr || locked.info()->platformInfo == nullptr) {
            return MINEMOD_RESULT_INVALID_ARGUMENT;
        }
        if (create_drawing_surface(object_at<VkInstance_T>(instance), *locked.info(), surface) != VK_SUCCESS) {
            return MINEMOD_RESULT_INTERNAL_ERROR;
        }
    }
    const std::array<jlong, 1> created{std::bit_cast<jlong>(surface)};
    return store_fields(*env, surface_field, created) ? MINEMOD_RESULT_OK : MINEMOD_RESULT_INVALID_ARGUMENT;
}

}

bool register_awt_surface_calls(JNIEnv& env) noexcept {
    const std::array methods{
        native_method("nativeCreate", "(JLjava/awt/Canvas;[J)I", &create),
    };
    return register_natives(env, "io/github/luasgraphicss/minemod/bridge/AwtSurfaces", methods);
}

}
