#ifndef MINEMOD_ABI_CAPABILITIES_H
#define MINEMOD_ABI_CAPABILITIES_H

#include <stdint.h>

typedef uint64_t minemod_capabilities;

#define MINEMOD_CAPABILITY_COMMAND_STREAM (UINT64_C(1) << 0)
#define MINEMOD_CAPABILITY_BUFFER_RESOURCES (UINT64_C(1) << 1)

#endif
