#include "support/descriptor_count.hpp"

#include <filesystem>
#include <iterator>

namespace minemod::interop::test_support {

std::ptrdiff_t open_descriptor_count() {
    const std::filesystem::directory_iterator descriptors("/proc/self/fd");
    return std::distance(begin(descriptors), end(descriptors));
}

}
