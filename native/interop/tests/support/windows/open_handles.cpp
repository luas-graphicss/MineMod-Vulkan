#include "support/open_handles.hpp"

#include <stdexcept>

#include <windows.h>

namespace minemod::interop::test_support {

std::int64_t open_handle() {
    return reinterpret_cast<std::intptr_t>(::CreateEventW(nullptr, TRUE, FALSE, nullptr));
}

bool is_handle_open(std::int64_t handle) {
    DWORD flags = 0;
    return ::GetHandleInformation(reinterpret_cast<HANDLE>(static_cast<std::intptr_t>(handle)), &flags) != FALSE;
}

std::ptrdiff_t open_handle_count() {
    DWORD count = 0;
    if (::GetProcessHandleCount(::GetCurrentProcess(), &count) == FALSE) {
        throw std::runtime_error("the handles of the process could not be counted");
    }
    return static_cast<std::ptrdiff_t>(count);
}

}
