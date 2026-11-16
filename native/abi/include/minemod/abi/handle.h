#ifndef MINEMOD_ABI_HANDLE_H
#define MINEMOD_ABI_HANDLE_H

#include <stdint.h>

typedef uint64_t minemod_handle;

#define MINEMOD_HANDLE_NULL UINT64_C(0)
#define MINEMOD_HANDLE_INDEX_BITS 32
#define MINEMOD_HANDLE_GENERATION_BITS 24
#define MINEMOD_HANDLE_MAX_GENERATION ((UINT32_C(1) << MINEMOD_HANDLE_GENERATION_BITS) - 1)

enum {
    MINEMOD_HANDLE_KIND_BUFFER = 1
};

static inline minemod_handle minemod_handle_pack(uint8_t kind, uint32_t generation, uint32_t index) {
    return ((minemod_handle)kind << (MINEMOD_HANDLE_INDEX_BITS + MINEMOD_HANDLE_GENERATION_BITS))
        | ((minemod_handle)(generation & MINEMOD_HANDLE_MAX_GENERATION) << MINEMOD_HANDLE_INDEX_BITS)
        | (minemod_handle)index;
}

static inline uint8_t minemod_handle_kind(minemod_handle handle) {
    return (uint8_t)(handle >> (MINEMOD_HANDLE_INDEX_BITS + MINEMOD_HANDLE_GENERATION_BITS));
}

static inline uint32_t minemod_handle_generation(minemod_handle handle) {
    return (uint32_t)(handle >> MINEMOD_HANDLE_INDEX_BITS) & MINEMOD_HANDLE_MAX_GENERATION;
}

static inline uint32_t minemod_handle_index(minemod_handle handle) {
    return (uint32_t)(handle & UINT32_MAX);
}

#endif
