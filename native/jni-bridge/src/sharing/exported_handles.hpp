#pragma once

#include <cstdint>

namespace minemod::jni {

bool close_exported_handle(std::int64_t handle) noexcept;

}
