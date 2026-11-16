#include "minemod/abi/negotiation.h"

#include "minemod/abi/command_stream.h"
#include "negotiation/abi_compatibility.hpp"

static_assert(sizeof(minemod_negotiation_request) == 16);
static_assert(sizeof(minemod_negotiation) == 32);

minemod_result minemod_negotiate(
    const minemod_negotiation_request* request, minemod_negotiation* negotiation) noexcept {
    if (request == nullptr || negotiation == nullptr
        || request->struct_bytes != sizeof(minemod_negotiation_request)) {
        return MINEMOD_RESULT_INVALID_ARGUMENT;
    }

    *negotiation = minemod_negotiation{
        .library_version = minemod::abi::library_version,
        .reserved = 0,
        .supported_capabilities = minemod::abi::supported_capabilities,
        .missing_capabilities = request->required_capabilities & ~minemod::abi::supported_capabilities,
        .max_stream_bytes = MINEMOD_STREAM_MAX_BYTES,
        .max_stream_commands = MINEMOD_STREAM_MAX_COMMANDS,
    };

    if (!minemod::abi::accepts_client(request->client_version)) {
        return MINEMOD_RESULT_INCOMPATIBLE_ABI;
    }
    if (negotiation->missing_capabilities != 0) {
        return MINEMOD_RESULT_UNSUPPORTED_CAPABILITY;
    }
    return MINEMOD_RESULT_OK;
}
