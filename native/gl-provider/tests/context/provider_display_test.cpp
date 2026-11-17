#include <string>

#include <doctest.h>

#include "context/provider_display.hpp"
#include "support/fake_provider_library.hpp"

namespace {

using namespace minemod::provider;
using minemod::provider::testing::open_fake_provider;
using minemod::provider::testing::symbol_as;

constexpr std::int32_t core_profile_bit = 0x1;
constexpr std::int32_t compatibility_profile_bit = 0x2;

const FakeContextRequest& request_of(const ProviderLibrary& library, EglHandle context) {
    return *symbol_as<FakeContextRequestOf>(library, "fake_provider_context_request")(context);
}

void* current_context(const ProviderLibrary& library) {
    return symbol_as<FakeCurrentContext>(library, "fake_provider_current_context")();
}

}

TEST_CASE("a context is created with the requested version and profile") {
    const ProviderLibrary library = open_fake_provider();
    const ProviderDisplay display(library);

    const EglHandle core = display.create_context({ContextProfile::core, 4, 5}, nullptr);
    const EglHandle compatibility = display.create_context({ContextProfile::compatibility, 3, 0}, nullptr);

    CHECK(request_of(library, core).major == 4);
    CHECK(request_of(library, core).minor == 5);
    CHECK(request_of(library, core).profile_mask == core_profile_bit);
    CHECK(request_of(library, core).shared_with == nullptr);
    CHECK(request_of(library, compatibility).major == 3);
    CHECK(request_of(library, compatibility).profile_mask == compatibility_profile_bit);
}

TEST_CASE("a sharing context names the context it shares with") {
    const ProviderLibrary library = open_fake_provider();
    const ProviderDisplay display(library);
    const EglHandle first = display.create_context({ContextProfile::core, 4, 5}, nullptr);

    const EglHandle second = display.create_context({ContextProfile::core, 4, 5}, first);

    CHECK(request_of(library, second).shared_with == first);
}

TEST_CASE("a version the provider cannot create is reported with its EGL error") {
    const ProviderLibrary library = open_fake_provider();
    const ProviderDisplay display(library);

    try {
        display.create_context({ContextProfile::core, 9, 0}, nullptr);
        FAIL("the context was created");
    } catch (const DisplayError& failure) {
        CHECK(std::string(failure.what()).find("0x3009") != std::string::npos);
    }
}

TEST_CASE("a context becomes current on the calling thread and is released again") {
    const ProviderLibrary library = open_fake_provider();
    const ProviderDisplay display(library);
    const EglHandle context = display.create_context({ContextProfile::core, 4, 5}, nullptr);

    display.make_current(context);
    CHECK(current_context(library) == context);

    display.release_current();
    CHECK(current_context(library) == nullptr);
}

TEST_CASE("a function is resolved by the provider's own lookup before its exported symbols") {
    const ProviderLibrary library = open_fake_provider();
    const ProviderDisplay display(library);

    CHECK(display.function_address("glGetError") == library.symbol("glGetError"));
    CHECK(display.function_address("glBindTexture") == library.symbol("glBindTexture"));
    CHECK(display.function_address("glNoSuchFunction") == nullptr);
}
