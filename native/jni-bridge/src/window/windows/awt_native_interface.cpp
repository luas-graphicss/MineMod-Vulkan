#include "window/awt_native_interface.hpp"

#include <windows.h>

namespace minemod::jni {

awt_entry_point find_awt_entry_point() noexcept {
    const HMODULE library = GetModuleHandleW(L"jawt.dll");
    if (library == nullptr) {
        return nullptr;
    }
    return reinterpret_cast<awt_entry_point>(GetProcAddress(library, "JAWT_GetAWT"));
}

}
