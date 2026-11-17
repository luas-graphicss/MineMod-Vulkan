#include "memory/exported_handle.hpp"

#include <unistd.h>

namespace minemod::interop {

void ExportedHandle::close_owned() noexcept {
    if (is_open()) {
        ::close(static_cast<int>(release()));
    }
}

}
