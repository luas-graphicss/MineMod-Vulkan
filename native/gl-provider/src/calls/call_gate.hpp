#pragma once

#include <cstdint>
#include <span>

namespace minemod::provider {

using Interceptor = std::int64_t (*)(std::uint32_t function, std::span<const std::int64_t> words) noexcept;

void bind_interceptor(Interceptor interceptor) noexcept;

std::int64_t intercept(std::uint32_t function, std::span<const std::int64_t> words) noexcept;

std::uint64_t unanswered_call_count() noexcept;

}
