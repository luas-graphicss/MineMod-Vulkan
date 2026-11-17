#include "library/dynamic_library.hpp"

#include <dlfcn.h>

#include <string>

#include "library/library_load_error.hpp"

namespace minemod::provider {

LibraryHandle open_dynamic_library(const std::filesystem::path& file) {
    void* handle = ::dlopen(file.c_str(), RTLD_NOW | RTLD_LOCAL);
    if (handle == nullptr) {
        const char* reason = ::dlerror();
        throw LibraryLoadError(
            file.filename().string() + " could not be loaded: " + (reason != nullptr ? reason : "unknown reason"));
    }
    return handle;
}

void close_dynamic_library(LibraryHandle library) noexcept {
    ::dlclose(library);
}

void* find_library_symbol(LibraryHandle library, const char* name) noexcept {
    return ::dlsym(library, name);
}

}
