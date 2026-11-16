#include "support/fake_provider_library.hpp"

#include <array>
#include <string>

namespace minemod::provider::testing {

std::filesystem::path fake_provider_directory() {
    return MINEMOD_FAKE_PROVIDER_DIRECTORY;
}

ProviderLibrary open_fake_provider() {
    const std::array<std::string, 1> dependencies{"libfake_dependency.so"};
    return ProviderLibrary(fake_provider_directory(), dependencies, "libfake_provider.so");
}

const FakeCall& last_call(const ProviderLibrary& library) {
    return *symbol_as<FakeLastCall>(library, "fake_provider_last_call")();
}

}
