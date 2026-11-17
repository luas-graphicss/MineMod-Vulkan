#include "library/dynamic_library.hpp"

#include <string>

#include <windows.h>

#include "library/library_load_error.hpp"

namespace minemod::provider {

LibraryHandle open_dynamic_library(const std::filesystem::path& file) {
    const HMODULE module = ::LoadLibraryExW(file.c_str(), nullptr, LOAD_LIBRARY_SEARCH_SYSTEM32);
    if (module == nullptr) {
        throw LibraryLoadError(
            file.filename().string() + " could not be loaded: Windows error " + std::to_string(::GetLastError()));
    }
    return module;
}

void close_dynamic_library(LibraryHandle library) noexcept {
    ::FreeLibrary(static_cast<HMODULE>(library));
}

void* find_library_symbol(LibraryHandle library, const char* name) noexcept {
    return reinterpret_cast<void*>(::GetProcAddress(static_cast<HMODULE>(library), name));
}

}
