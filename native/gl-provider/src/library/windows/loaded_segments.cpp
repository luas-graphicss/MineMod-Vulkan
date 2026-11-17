#include "library/loaded_image.hpp"

#include <cstddef>
#include <string>
#include <system_error>

#include <windows.h>

#include <psapi.h>

#include "library/library_load_error.hpp"

namespace minemod::provider {

namespace {

constexpr std::size_t expected_module_count = 256;

[[noreturn]] void fail(const char* operation) {
    throw LibraryLoadError(std::string(operation) + " failed with Windows error " + std::to_string(::GetLastError()));
}

std::vector<HMODULE> loaded_modules() {
    std::vector<HMODULE> modules(expected_module_count);
    for (;;) {
        const auto offered_bytes = static_cast<DWORD>(modules.size() * sizeof(HMODULE));
        DWORD needed_bytes = 0;
        if (::EnumProcessModules(::GetCurrentProcess(), modules.data(), offered_bytes, &needed_bytes) == FALSE) {
            fail("listing the modules of the process");
        }
        const std::size_t needed = needed_bytes / sizeof(HMODULE);
        const bool is_complete = needed <= modules.size();
        modules.resize(needed);
        if (is_complete) {
            return modules;
        }
    }
}

std::filesystem::path file_of(HMODULE module) {
    std::wstring name(MAX_PATH, L'\0');
    for (;;) {
        const DWORD length = ::GetModuleFileNameW(module, name.data(), static_cast<DWORD>(name.size()));
        if (length < name.size()) {
            name.resize(length);
            return name;
        }
        name.resize(name.size() * 2);
    }
}

bool is_loaded_from(const std::filesystem::path& directory, const std::filesystem::path& file) {
    std::error_code failure;
    return !file.empty() && std::filesystem::equivalent(file.parent_path(), directory, failure) && !failure;
}

ImageSegment segment_of(HMODULE module) {
    MODULEINFO loaded{};
    if (::GetModuleInformation(::GetCurrentProcess(), module, &loaded, sizeof(loaded)) == FALSE) {
        fail("measuring a module of the provider");
    }
    const auto begin = reinterpret_cast<std::uintptr_t>(loaded.lpBaseOfDll);
    return ImageSegment{begin, begin + loaded.SizeOfImage};
}

}

std::vector<ImageSegment> segments_loaded_from(const std::filesystem::path& directory) {
    std::vector<ImageSegment> segments;
    for (const HMODULE module : loaded_modules()) {
        if (is_loaded_from(directory, file_of(module))) {
            segments.push_back(segment_of(module));
        }
    }
    return segments;
}

}
