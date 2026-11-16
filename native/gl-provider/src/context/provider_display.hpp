#pragma once

#include <cstdint>
#include <stdexcept>

#include "context/egl_entry_points.hpp"
#include "library/provider_library.hpp"

namespace minemod::provider {

enum class ContextProfile : std::uint8_t {
    core,
    compatibility,
};

struct ContextRequest {
    ContextProfile profile;
    std::int32_t major;
    std::int32_t minor;
};

class DisplayError : public std::runtime_error {
public:
    using std::runtime_error::runtime_error;
};

class ProviderDisplay {
public:
    explicit ProviderDisplay(const ProviderLibrary& library);
    ProviderDisplay(const ProviderDisplay&) = delete;
    ProviderDisplay& operator=(const ProviderDisplay&) = delete;
    ~ProviderDisplay();

    EglHandle create_context(const ContextRequest& request, EglHandle shared_with) const;
    void destroy_context(EglHandle context) const noexcept;
    void make_current(EglHandle context) const;
    void release_current() const;
    void* function_address(const char* name) const noexcept;

private:
    [[noreturn]] void fail(const char* operation) const;

    const ProviderLibrary& library_;
    EglEntryPoints egl_;
    EglHandle display_ = nullptr;
};

}
