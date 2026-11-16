#include "context/egl_entry_points.hpp"

#include <string>

namespace minemod::provider {

namespace {

template <typename Function>
void resolve(const ProviderLibrary& library, const char* name, Function& entry_point) {
    void* address = library.symbol(name);
    if (address == nullptr) {
        throw LibraryLoadError(std::string(name) + " is not exported by the provider");
    }
    entry_point = reinterpret_cast<Function>(address);
}

}

EglEntryPoints resolve_egl_entry_points(const ProviderLibrary& library) {
    EglEntryPoints egl{};
    resolve(library, "eglGetPlatformDisplay", egl.get_platform_display);
    resolve(library, "eglInitialize", egl.initialize);
    resolve(library, "eglTerminate", egl.terminate);
    resolve(library, "eglQueryString", egl.query_string);
    resolve(library, "eglBindAPI", egl.bind_api);
    resolve(library, "eglCreateContext", egl.create_context);
    resolve(library, "eglDestroyContext", egl.destroy_context);
    resolve(library, "eglMakeCurrent", egl.make_current);
    resolve(library, "eglGetError", egl.get_error);
    resolve(library, "eglGetProcAddress", egl.get_proc_address);
    return egl;
}

}
