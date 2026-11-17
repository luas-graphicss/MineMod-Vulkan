#pragma once

#include <cstddef>
#include <cstdint>

namespace minemod::interop::test_support {

std::int64_t open_handle();
bool is_handle_open(std::int64_t handle);
std::ptrdiff_t open_handle_count();

}
