#pragma once

#include <cstddef>
#include <cstdint>

struct FakeCall {
    const char* function;
    std::int64_t words[12];
    std::size_t word_count;
};

struct FakeContextRequest {
    std::int32_t major;
    std::int32_t minor;
    std::int32_t profile_mask;
    void* shared_with;
};

using FakeLastCall = const FakeCall* (*)();
using FakeContextRequestOf = const FakeContextRequest* (*)(void* context);
using FakeCurrentContext = void* (*)();
