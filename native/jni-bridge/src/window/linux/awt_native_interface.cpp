#include "window/awt_native_interface.hpp"

#include <dlfcn.h>

namespace minemod::jni {

awt_entry_point find_awt_entry_point() noexcept {
    void* loaded_by_the_jvm = dlopen("libjawt.so", RTLD_LAZY | RTLD_NOLOAD);
    if (loaded_by_the_jvm == nullptr) {
        return nullptr;
    }
    void* entry = dlsym(loaded_by_the_jvm, "JAWT_GetAWT");
    dlclose(loaded_by_the_jvm);
    return reinterpret_cast<awt_entry_point>(entry);
}

}
