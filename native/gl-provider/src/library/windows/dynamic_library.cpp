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

void prepare_for_driver_threads(LibraryHandle library) {
    HMODULE kept = nullptr;
    const DWORD until_exit = GET_MODULE_HANDLE_EX_FLAG_PIN | GET_MODULE_HANDLE_EX_FLAG_FROM_ADDRESS;
    if (::GetModuleHandleExW(until_exit, static_cast<LPCWSTR>(library), &kept) == 0) {
        throw LibraryLoadError(
            "a provider library could not be kept loaded: Windows error " + std::to_string(::GetLastError()));
    }
}

void* find_library_symbol(LibraryHandle library, const char* name) noexcept {
    return reinterpret_cast<void*>(::GetProcAddress(static_cast<HMODULE>(library), name));
}

}
