#ifndef MINEMOD_ABI_NEGOTIATION_H
#define MINEMOD_ABI_NEGOTIATION_H

#include <stdint.h>

#include "minemod/abi/capabilities.h"
#include "minemod/abi/linkage.h"
#include "minemod/abi/result.h"
#include "minemod/abi/version.h"

typedef struct minemod_negotiation_request {
    uint32_t struct_bytes;
    minemod_abi_version client_version;
    minemod_capabilities required_capabilities;
} minemod_negotiation_request;

typedef struct minemod_negotiation {
    minemod_abi_version library_version;
    uint32_t reserved;
    minemod_capabilities supported_capabilities;
    minemod_capabilities missing_capabilities;
    uint32_t max_stream_bytes;
    uint32_t max_stream_commands;
} minemod_negotiation;

MINEMOD_ABI_BEGIN

minemod_result minemod_negotiate(
    const minemod_negotiation_request* request, minemod_negotiation* negotiation) MINEMOD_ABI_NOEXCEPT;

MINEMOD_ABI_END

#endif
