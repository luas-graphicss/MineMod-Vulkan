#include "memory/exported_handle.hpp"

namespace minemod::interop {

ExportedHandle& ExportedHandle::operator=(ExportedHandle&& other) noexcept {
    if (this != &other) {
        close_owned();
        handle_ = other.release();
    }
    return *this;
}

ExportedHandle::~ExportedHandle() {
    close_owned();
}

}
