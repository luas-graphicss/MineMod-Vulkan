#include "sharing/exported_handles.hpp"

#include <limits>

#include <unistd.h>

namespace minemod::jni {

bool close_exported_handle(std::int64_t handle) noexcept {
    if (handle < 0 || handle > std::numeric_limits<int>::max()) {
        return false;
    }
    return ::close(static_cast<int>(handle)) == 0;
}

}
