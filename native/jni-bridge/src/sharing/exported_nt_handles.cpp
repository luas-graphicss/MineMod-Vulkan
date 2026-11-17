#include "sharing/exported_handles.hpp"

#include <windows.h>

namespace minemod::jni {

bool close_exported_handle(std::int64_t handle) noexcept {
    if (handle <= 0) {
        return false;
    }
    return ::CloseHandle(reinterpret_cast<HANDLE>(static_cast<std::intptr_t>(handle))) != FALSE;
}

}
