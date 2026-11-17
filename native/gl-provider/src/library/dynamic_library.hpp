#pragma once

#include <filesystem>

namespace minemod::provider {

using LibraryHandle = void*;

LibraryHandle open_dynamic_library(const std::filesystem::path& file);
void close_dynamic_library(LibraryHandle library) noexcept;
void prepare_for_driver_threads(LibraryHandle library);
void* find_library_symbol(LibraryHandle library, const char* name) noexcept;

}
