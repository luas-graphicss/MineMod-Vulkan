#include <array>
#include <cstdint>
#include <vector>

#include <doctest.h>

#include "calls/call_gate.hpp"
#include "calls/call_table.hpp"
#include "calls/call_words.hpp"

namespace {

using namespace minemod::provider;

struct Interception {
    std::uint32_t function = 0;
    std::vector<std::int64_t> words;
};

Interception intercepted;
std::int64_t scripted_answer = 0;

std::int64_t record_interception(std::uint32_t function, std::span<const std::int64_t> words) noexcept {
    intercepted.function = function;
    intercepted.words.assign(words.begin(), words.end());
    return scripted_answer;
}

class BoundInterceptor {
public:
    explicit BoundInterceptor(std::int64_t answer) {
        scripted_answer = answer;
        intercepted = Interception{};
        bind_interceptor(&record_interception);
    }
    BoundInterceptor(const BoundInterceptor&) = delete;
    BoundInterceptor& operator=(const BoundInterceptor&) = delete;
    ~BoundInterceptor() {
        bind_interceptor(nullptr);
    }
};

template <typename Function>
Function thunk_of(const char* name) {
    return reinterpret_cast<Function>(call_records()[*find_call(name)].thunk);
}

}

TEST_CASE("a thunk hands its function and its arguments to the interceptor") {
    const BoundInterceptor bound(0);

    thunk_of<void (*)(std::uint32_t, std::uint32_t)>("glBindTexture")(0x0DE1, 9);

    CHECK(intercepted.function == *find_call("glBindTexture"));
    CHECK(intercepted.words == std::vector<std::int64_t>{0x0DE1, 9});
}

TEST_CASE("a thunk packs floating point arguments bit for bit") {
    const BoundInterceptor bound(0);

    thunk_of<void (*)(float, float, float, float)>("glClearColor")(0.25F, 0.5F, -1.0F, 1.0F);
    REQUIRE(intercepted.words.size() == 4);
    CHECK(unpack<float>(intercepted.words[0]) == 0.25F);
    CHECK(unpack<float>(intercepted.words[2]) == -1.0F);

    thunk_of<void (*)(double, double, double)>("glVertex3d")(1.5, -2.25, 1.0e300);
    REQUIRE(intercepted.words.size() == 3);
    CHECK(unpack<double>(intercepted.words[1]) == -2.25);
    CHECK(unpack<double>(intercepted.words[2]) == 1.0e300);
}

TEST_CASE("a thunk keeps the order of arguments that do not fit in registers") {
    const BoundInterceptor bound(0);
    const std::array<std::uint8_t, 4> pixels{};
    using TexImage2D = void (*)(
        std::uint32_t, std::int32_t, std::int32_t, std::int32_t, std::int32_t, std::int32_t, std::uint32_t,
        std::uint32_t, const void*);

    thunk_of<TexImage2D>("glTexImage2D")(0x0DE1, 1, 0x8058, 64, 32, 0, 0x1908, 0x1401, pixels.data());

    CHECK(intercepted.words == std::vector<std::int64_t>{0x0DE1, 1, 0x8058, 64, 32, 0, 0x1908, 0x1401, pack(pixels.data())});
}

TEST_CASE("a thunk returns the interceptor's answer in the type of the function") {
    {
        const BoundInterceptor bound(1);
        CHECK(thunk_of<std::uint8_t (*)(std::uint32_t)>("glIsTexture")(4) == 1);
    }
    {
        const BoundInterceptor bound(0x0502);
        CHECK(thunk_of<std::uint32_t (*)()>("glGetError")() == 0x0502);
        CHECK(intercepted.words.empty());
    }
    {
        int storage = 0;
        const BoundInterceptor bound(pack(&storage));
        CHECK(thunk_of<void* (*)(std::uint32_t, std::uint32_t)>("glMapBuffer")(0x8892, 0x88B9) == &storage);
    }
}

TEST_CASE("a call that arrives while nothing intercepts is counted and answered with zero") {
    const std::uint64_t before = unanswered_call_count();

    CHECK(thunk_of<std::uint32_t (*)()>("glGetError")() == 0);

    CHECK(unanswered_call_count() == before + 1);
}
