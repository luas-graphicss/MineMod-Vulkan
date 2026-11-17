#include "sharing/exported_handles.hpp"

namespace minemod::jni {

bool close_exported_handle(std::int64_t) noexcept {
    return false;
}

}
