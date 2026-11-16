#include "calls/call_gate.hpp"

#include <atomic>

namespace minemod::provider {

namespace {

std::atomic<Interceptor> bound_interceptor{nullptr};
std::atomic<std::uint64_t> unanswered_calls{0};

}

void bind_interceptor(Interceptor interceptor) noexcept {
    bound_interceptor.store(interceptor);
}

std::int64_t intercept(std::uint32_t function, std::span<const std::int64_t> words) noexcept {
    const Interceptor interceptor = bound_interceptor.load();
    if (interceptor == nullptr) {
        unanswered_calls.fetch_add(1);
        return 0;
    }
    return interceptor(function, words);
}

std::uint64_t unanswered_call_count() noexcept {
    return unanswered_calls.load();
}

}
