#include "library/provider_library.hpp"

#include <dlfcn.h>

#include <utility>

namespace minemod::provider {

namespace {

std::filesystem::path private_directory(const std::filesystem::path& directory) {
    std::error_code failure;
    std::filesystem::path resolved = std::filesystem::canonical(directory, failure);
    if (failure || !std::filesystem::is_directory(resolved, failure)) {
        throw LibraryLoadError("the provider directory does not exist: " + directory.string());
    }
    return resolved;
}

std::filesystem::path library_in(const std::filesystem::path& directory, const std::string& file_name) {
    const std::filesystem::path name(file_name);
    if (file_name.empty() || name.filename() != name || name == "." || name == "..") {
        throw LibraryLoadError("a provider library is named by file name only: " + file_name);
    }
    const std::filesystem::path library = directory / name;
    std::error_code failure;
    if (!std::filesystem::is_regular_file(std::filesystem::symlink_status(library, failure))) {
        throw LibraryLoadError(file_name + " is not a regular file in the provider directory");
    }
    return library;
}

}

ProviderLibrary::ProviderLibrary(
    const std::filesystem::path& directory, std::span<const std::string> dependencies, const std::string& entry)
    : directory_(private_directory(directory)) {
    try {
        handles_.reserve(dependencies.size() + 1);
        for (const std::string& dependency : dependencies) {
            open_file(dependency);
        }
        open_file(entry);
    } catch (...) {
        close_all();
        throw;
    }
}

ProviderLibrary::ProviderLibrary(ProviderLibrary&& other) noexcept
    : directory_(std::move(other.directory_)), handles_(std::exchange(other.handles_, {})) {}

ProviderLibrary& ProviderLibrary::operator=(ProviderLibrary&& other) noexcept {
    if (this != &other) {
        close_all();
        directory_ = std::move(other.directory_);
        handles_ = std::exchange(other.handles_, {});
    }
    return *this;
}

ProviderLibrary::~ProviderLibrary() {
    close_all();
}

void* ProviderLibrary::symbol(const char* name) const noexcept {
    return handles_.empty() ? nullptr : ::dlsym(handles_.back(), name);
}

void ProviderLibrary::open_file(const std::string& file_name) {
    const std::filesystem::path library = library_in(directory_, file_name);
    void* handle = ::dlopen(library.c_str(), RTLD_NOW | RTLD_LOCAL);
    if (handle == nullptr) {
        const char* reason = ::dlerror();
        throw LibraryLoadError(file_name + " could not be loaded: " + (reason != nullptr ? reason : "unknown reason"));
    }
    handles_.push_back(handle);
}

void ProviderLibrary::close_all() noexcept {
    while (!handles_.empty()) {
        ::dlclose(handles_.back());
        handles_.pop_back();
    }
}

}
