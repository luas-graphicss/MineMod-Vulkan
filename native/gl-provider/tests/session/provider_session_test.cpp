#include <array>
#include <string>

#include <doctest.h>

#include "calls/call_table.hpp"
#include "session/provider_session.hpp"
#include "support/fake_provider_library.hpp"

namespace {

using namespace minemod::provider;
using minemod::provider::testing::fake_dependencies;
using minemod::provider::testing::fake_provider_directory;
using minemod::provider::testing::fake_provider_file;

const std::array<std::string, 1> dependencies = fake_dependencies();

std::string failure_of(ProviderSession& session, const char* function, std::span<const std::int64_t> words) {
    try {
        session.invoke(*find_call(function), words);
    } catch (const CallError& failure) {
        return failure.what();
    }
    return {};
}

}

TEST_CASE("a session invokes functions the provider offers from inside its own image") {
    ProviderSession session(fake_provider_directory(), dependencies, fake_provider_file());
    std::array<std::uint32_t, 1> name{};

    const std::array<std::int64_t, 2> generate{1, static_cast<std::int64_t>(reinterpret_cast<std::uintptr_t>(name.data()))};
    session.invoke(*find_call("glGenTextures"), generate);

    CHECK(name[0] == 42);
    CHECK(session.invoke(*find_call("glGetError"), {}) == 0x0502);
    CHECK(contains_address(
        {session.image().begin(), session.image().end()},
        reinterpret_cast<std::uintptr_t>(session.function_address("glGetError"))));
}

TEST_CASE("a session refuses a function the provider lacks, a wrong argument count and an unknown index") {
    ProviderSession session(fake_provider_directory(), dependencies, fake_provider_file());
    const std::array<std::int64_t, 1> one_word{1};

    CHECK(failure_of(session, "glEnable", one_word) == "glEnable is not offered by the provider");
    CHECK(failure_of(session, "glBindTexture", one_word) == "glBindTexture takes 2 arguments, not 1");
    CHECK_THROWS_AS(session.invoke(static_cast<std::uint32_t>(call_records().size()), {}), CallError);
}
