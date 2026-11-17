#pragma once

#include <filesystem>
#include <span>
#include <string>
#include <vector>

#include "library/dynamic_library.hpp"
#include "library/library_load_error.hpp"

namespace minemod::provider {

class ProviderLibrary {
public:
    ProviderLibrary(
        const std::filesystem::path& directory, std::span<const std::string> dependencies, const std::string& entry);
    ProviderLibrary(ProviderLibrary&& other) noexcept;
    ProviderLibrary& operator=(ProviderLibrary&& other) noexcept;
    ProviderLibrary(const ProviderLibrary&) = delete;
    ProviderLibrary& operator=(const ProviderLibrary&) = delete;
    ~ProviderLibrary();

    void prepare_for_driver_threads();
    void* symbol(const char* name) const noexcept;

    const std::filesystem::path& directory() const noexcept {
        return directory_;
    }

private:
    void open_file(const std::string& file_name);
    void close_all() noexcept;

    std::filesystem::path directory_;
    std::vector<LibraryHandle> handles_;
};

}
