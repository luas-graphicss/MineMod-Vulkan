#pragma once

#include <atomic>
#include <cstdint>
#include <filesystem>
#include <span>
#include <stdexcept>
#include <string>
#include <vector>

#include "context/provider_display.hpp"
#include "library/loaded_image.hpp"
#include "library/provider_library.hpp"

namespace minemod::provider {

class CallError : public std::runtime_error {
public:
    using std::runtime_error::runtime_error;
};

class ProviderSession {
public:
    ProviderSession(
        const std::filesystem::path& directory, std::span<const std::string> dependencies, const std::string& entry);
    ProviderSession(const ProviderSession&) = delete;
    ProviderSession& operator=(const ProviderSession&) = delete;

    const ProviderDisplay& display() const noexcept {
        return display_;
    }

    std::span<const ImageSegment> image() const noexcept {
        return image_;
    }

    void* function_address(const char* name) const noexcept {
        return display_.function_address(name);
    }

    std::int64_t invoke(std::uint32_t function, std::span<const std::int64_t> words);

private:
    void* provided_address(std::uint32_t function);

    ProviderLibrary library_;
    ProviderDisplay display_;
    std::vector<ImageSegment> image_;
    std::vector<std::atomic<void*>> provided_;
};

}
