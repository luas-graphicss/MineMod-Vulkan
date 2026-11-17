#include "memory/exported_handle.hpp"

#include <windows.h>

namespace minemod::interop {

void ExportedHandle::close_owned() noexcept {
    if (is_open()) {
        ::CloseHandle(reinterpret_cast<HANDLE>(static_cast<std::intptr_t>(release())));
    }
}

}
