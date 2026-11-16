#pragma once

#include <cstdint>

#include "fake_provider/fake_calls.hpp"
#include "library/provider_library.hpp"

namespace minemod::provider::testing {

std::filesystem::path fake_provider_directory();

ProviderLibrary open_fake_provider();

const FakeCall& last_call(const ProviderLibrary& library);

template <typename Function>
Function symbol_as(const ProviderLibrary& library, const char* name) {
    return reinterpret_cast<Function>(library.symbol(name));
}

}
