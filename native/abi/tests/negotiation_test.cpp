#include <doctest.h>

#include "minemod/abi/command_stream.h"
#include "minemod/abi/negotiation.h"

namespace {

minemod_negotiation_request request_for(minemod_abi_version version, minemod_capabilities required) {
    return minemod_negotiation_request{
        .struct_bytes = sizeof(minemod_negotiation_request),
        .client_version = version,
        .required_capabilities = required,
    };
}

constexpr minemod_abi_version library_version{MINEMOD_ABI_VERSION_MAJOR, MINEMOD_ABI_VERSION_MINOR};
constexpr minemod_capabilities implemented_capabilities =
    MINEMOD_CAPABILITY_COMMAND_STREAM | MINEMOD_CAPABILITY_BUFFER_RESOURCES;

}

TEST_CASE("negotiation accepts the library version and reports its limits") {
    const auto request = request_for(library_version, implemented_capabilities);
    minemod_negotiation negotiation{};

    REQUIRE(minemod_negotiate(&request, &negotiation) == MINEMOD_RESULT_OK);

    CHECK(negotiation.library_version.major == MINEMOD_ABI_VERSION_MAJOR);
    CHECK(negotiation.library_version.minor == MINEMOD_ABI_VERSION_MINOR);
    CHECK(negotiation.missing_capabilities == 0);
    CHECK(negotiation.max_stream_bytes == MINEMOD_STREAM_MAX_BYTES);
    CHECK(negotiation.max_stream_commands == MINEMOD_STREAM_MAX_COMMANDS);
}

TEST_CASE("negotiation advertises exactly the capabilities the session tests exercise") {
    const auto request = request_for(library_version, 0);
    minemod_negotiation negotiation{};

    REQUIRE(minemod_negotiate(&request, &negotiation) == MINEMOD_RESULT_OK);

    CHECK(negotiation.supported_capabilities == implemented_capabilities);
}

TEST_CASE("negotiation rejects another ABI generation and still names the library version") {
    minemod_negotiation negotiation{};

    SUBCASE("different major") {
        const auto request = request_for({MINEMOD_ABI_VERSION_MAJOR + 1, MINEMOD_ABI_VERSION_MINOR}, 0);
        CHECK(minemod_negotiate(&request, &negotiation) == MINEMOD_RESULT_INCOMPATIBLE_ABI);
    }
    SUBCASE("different minor while the ABI is unstable") {
        const auto request = request_for({MINEMOD_ABI_VERSION_MAJOR, MINEMOD_ABI_VERSION_MINOR + 1}, 0);
        CHECK(minemod_negotiate(&request, &negotiation) == MINEMOD_RESULT_INCOMPATIBLE_ABI);
    }

    CHECK(negotiation.library_version.major == MINEMOD_ABI_VERSION_MAJOR);
    CHECK(negotiation.library_version.minor == MINEMOD_ABI_VERSION_MINOR);
}

TEST_CASE("negotiation names the capabilities it cannot provide") {
    const minemod_capabilities unimplemented = UINT64_C(1) << 63;
    const auto request = request_for(library_version, implemented_capabilities | unimplemented);
    minemod_negotiation negotiation{};

    CHECK(minemod_negotiate(&request, &negotiation) == MINEMOD_RESULT_UNSUPPORTED_CAPABILITY);
    CHECK(negotiation.missing_capabilities == unimplemented);
}

TEST_CASE("negotiation rejects malformed requests") {
    auto request = request_for(library_version, 0);
    minemod_negotiation negotiation{};

    CHECK(minemod_negotiate(nullptr, &negotiation) == MINEMOD_RESULT_INVALID_ARGUMENT);
    CHECK(minemod_negotiate(&request, nullptr) == MINEMOD_RESULT_INVALID_ARGUMENT);

    request.struct_bytes = sizeof(minemod_negotiation_request) - 4;
    CHECK(minemod_negotiate(&request, &negotiation) == MINEMOD_RESULT_INVALID_ARGUMENT);
}
