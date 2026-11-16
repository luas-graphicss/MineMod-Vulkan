#include <stddef.h>
#include <stdint.h>

#include "minemod/abi/capabilities.h"
#include "minemod/abi/command_stream.h"
#include "minemod/abi/handle.h"
#include "minemod/abi/negotiation.h"
#include "minemod/abi/result.h"
#include "minemod/abi/session.h"
#include "minemod/abi/version.h"

_Static_assert(sizeof(minemod_abi_version) == 4, "minemod_abi_version changed its layout");
_Static_assert(sizeof(minemod_negotiation_request) == 16, "minemod_negotiation_request changed its layout");
_Static_assert(sizeof(minemod_negotiation) == 32, "minemod_negotiation changed its layout");
_Static_assert(sizeof(minemod_stream_report) == 16, "minemod_stream_report changed its layout");
_Static_assert(sizeof(minemod_session_config) == 32, "minemod_session_config changed its layout");
_Static_assert(sizeof(minemod_session_counters) == 64, "minemod_session_counters changed its layout");
_Static_assert(sizeof(minemod_buffer_description) == 24, "minemod_buffer_description changed its layout");
_Static_assert(offsetof(minemod_session_config, max_buffer_bytes) == 24, "minemod_session_config has padding");

enum {
    EXIT_NEGOTIATION_FAILED = 1,
    EXIT_HANDLE_LAYOUT_BROKEN = 2,
    EXIT_SESSION_FAILED = 3
};

int main(void) {
    minemod_negotiation_request request = {
        (uint32_t)sizeof(minemod_negotiation_request),
        {MINEMOD_ABI_VERSION_MAJOR, MINEMOD_ABI_VERSION_MINOR},
        MINEMOD_CAPABILITY_COMMAND_STREAM | MINEMOD_CAPABILITY_BUFFER_RESOURCES,
    };
    minemod_negotiation negotiation;
    if (minemod_negotiate(&request, &negotiation) != MINEMOD_RESULT_OK) {
        return EXIT_NEGOTIATION_FAILED;
    }

    const minemod_handle handle = minemod_handle_pack(MINEMOD_HANDLE_KIND_BUFFER, 7, 42);
    if (minemod_handle_kind(handle) != MINEMOD_HANDLE_KIND_BUFFER
        || minemod_handle_generation(handle) != 7
        || minemod_handle_index(handle) != 42) {
        return EXIT_HANDLE_LAYOUT_BROKEN;
    }

    minemod_session_config config = {
        (uint32_t)sizeof(minemod_session_config),
        negotiation.library_version,
        request.required_capabilities,
        8,
        0,
        1024,
    };
    minemod_session* session = NULL;
    if (minemod_session_create(&config, &session) != MINEMOD_RESULT_OK) {
        return EXIT_SESSION_FAILED;
    }
    minemod_session_destroy(session);
    return 0;
}
