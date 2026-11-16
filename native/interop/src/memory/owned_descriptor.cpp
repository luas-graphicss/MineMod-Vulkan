#include "memory/owned_descriptor.hpp"

#include <unistd.h>

namespace minemod::interop {

OwnedDescriptor& OwnedDescriptor::operator=(OwnedDescriptor&& other) noexcept {
    if (this != &other) {
        close_owned();
        descriptor_ = other.release();
    }
    return *this;
}

OwnedDescriptor::~OwnedDescriptor() {
    close_owned();
}

void OwnedDescriptor::close_owned() noexcept {
    if (is_open()) {
        ::close(release());
    }
}

}
