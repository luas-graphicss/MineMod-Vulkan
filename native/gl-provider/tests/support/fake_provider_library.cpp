#include "support/fake_provider_library.hpp"

namespace minemod::provider::testing {

std::filesystem::path fake_provider_directory() {
    return MINEMOD_FAKE_PROVIDER_DIRECTORY;
}

std::string fake_provider_file() {
    return MINEMOD_FAKE_PROVIDER_FILE;
}

std::array<std::string, 1> fake_dependencies() {
    return {MINEMOD_FAKE_DEPENDENCY_FILE};
}

ProviderLibrary open_fake_provider() {
    const std::array<std::string, 1> dependencies = fake_dependencies();
    return ProviderLibrary(fake_provider_directory(), dependencies, fake_provider_file());
}

const FakeCall& last_call(const ProviderLibrary& library) {
    return *symbol_as<FakeLastCall>(library, "fake_provider_last_call")();
}

}
