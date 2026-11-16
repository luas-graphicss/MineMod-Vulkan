#ifndef MINEMOD_ABI_VERSION_H
#define MINEMOD_ABI_VERSION_H

#include <stdint.h>

#define MINEMOD_ABI_VERSION_MAJOR 0
#define MINEMOD_ABI_VERSION_MINOR 1

typedef struct minemod_abi_version {
    uint16_t major;
    uint16_t minor;
} minemod_abi_version;

#endif
