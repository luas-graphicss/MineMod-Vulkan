#ifndef MINEMOD_SHADER_SERVICE_H
#define MINEMOD_SHADER_SERVICE_H

#include <stdint.h>

#include "minemod/abi/linkage.h"
#include "minemod/abi/result.h"
#include "minemod/abi/version.h"

#define MINEMOD_SHADER_MIN_WALL_TIME_MILLISECONDS UINT32_C(100)
#define MINEMOD_SHADER_MAX_WALL_TIME_MILLISECONDS UINT32_C(60000)
#define MINEMOD_SHADER_MIN_COMPILER_MEMORY_BYTES (UINT64_C(1) << 26)
#define MINEMOD_SHADER_MAX_COMPILER_MEMORY_BYTES (UINT64_C(1) << 32)
#define MINEMOD_SHADER_MAX_CACHE_BYTES (UINT64_C(1) << 28)
#define MINEMOD_SHADER_MAX_PATH_BYTES UINT64_C(4096)
#define MINEMOD_SHADER_MAX_INCLUDES UINT32_C(64)
#define MINEMOD_SHADER_MAX_SOURCE_BYTES (UINT64_C(1) << 18)
#define MINEMOD_SHADER_MAX_NAME_BYTES UINT64_C(256)

enum {
    MINEMOD_SHADER_STAGE_VERTEX = 0,
    MINEMOD_SHADER_STAGE_FRAGMENT = 1
};

typedef struct minemod_shader_service minemod_shader_service;
typedef struct minemod_shader_compilation minemod_shader_compilation;

typedef struct minemod_shader_service_config {
    uint32_t struct_bytes;
    minemod_abi_version abi_version;
    uint32_t wall_time_milliseconds;
    uint32_t reserved;
    uint64_t compiler_memory_bytes;
    uint64_t cache_bytes;
    const char* compiler_path;
    const char* scratch_directory;
} minemod_shader_service_config;

typedef struct minemod_shader_text {
    const char* bytes;
    uint64_t length;
} minemod_shader_text;

typedef struct minemod_shader_include {
    minemod_shader_text key;
    minemod_shader_text text;
} minemod_shader_include;

typedef struct minemod_core_shader_source {
    uint32_t struct_bytes;
    uint32_t include_count;
    minemod_shader_text name;
    minemod_shader_text vertex;
    minemod_shader_text fragment;
    const minemod_shader_include* includes;
} minemod_core_shader_source;

MINEMOD_ABI_BEGIN

minemod_result minemod_shader_service_create(
    const minemod_shader_service_config* config, minemod_shader_service** service) MINEMOD_ABI_NOEXCEPT;

void minemod_shader_service_destroy(minemod_shader_service* service) MINEMOD_ABI_NOEXCEPT;

minemod_result minemod_shader_service_compile_core_shader(
    minemod_shader_service* service,
    const minemod_core_shader_source* source,
    minemod_shader_compilation** compilation) MINEMOD_ABI_NOEXCEPT;

void minemod_shader_compilation_destroy(minemod_shader_compilation* compilation) MINEMOD_ABI_NOEXCEPT;

uint32_t minemod_shader_compilation_succeeded(const minemod_shader_compilation* compilation) MINEMOD_ABI_NOEXCEPT;

uint64_t minemod_shader_compilation_read_words(
    const minemod_shader_compilation* compilation,
    uint32_t stage,
    uint32_t* destination,
    uint64_t capacity) MINEMOD_ABI_NOEXCEPT;

uint64_t minemod_shader_compilation_read_text(
    const minemod_shader_compilation* compilation, char* destination, uint64_t capacity) MINEMOD_ABI_NOEXCEPT;

MINEMOD_ABI_END

#endif
