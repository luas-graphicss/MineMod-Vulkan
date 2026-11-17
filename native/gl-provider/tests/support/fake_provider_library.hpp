#pragma once

#include <array>
#include <cstdint>
#include <string>

#include "fake_provider/fake_calls.hpp"
#include "library/provider_library.hpp"

namespace minemod::provider::testing {

std::filesystem::path fake_provider_directory();

std::string fake_provider_file();

std::array<std::string, 1> fake_dependencies();

ProviderLibrary open_fake_provider();

const FakeCall& last_call(const ProviderLibrary& library);

template <typename Function>
Function symbol_as(const ProviderLibrary& library, const char* name) {
    return reinterpret_cast<Function>(library.symbol(name));
}

}
