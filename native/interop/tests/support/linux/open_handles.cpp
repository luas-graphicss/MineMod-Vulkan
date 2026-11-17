#include "support/open_handles.hpp"

#include <fcntl.h>

#include <filesystem>
#include <iterator>

namespace minemod::interop::test_support {

std::int64_t open_handle() {
    return ::open("/dev/null", O_RDONLY | O_CLOEXEC);
}

bool is_handle_open(std::int64_t handle) {
    return ::fcntl(static_cast<int>(handle), F_GETFD) != -1;
}

std::ptrdiff_t open_handle_count() {
    const std::filesystem::directory_iterator descriptors("/proc/self/fd");
    return std::distance(begin(descriptors), end(descriptors));
}

}
