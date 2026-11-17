#include "session/provider_session.hpp"

#include "calls/call_table.hpp"

namespace minemod::provider {

namespace {

ProviderLibrary library_for_driver(
    const std::filesystem::path& directory, std::span<const std::string> dependencies, const std::string& entry) {
    ProviderLibrary library(directory, dependencies, entry);
    library.prepare_for_driver_threads();
    return library;
}

}

ProviderSession::ProviderSession(
    const std::filesystem::path& directory, std::span<const std::string> dependencies, const std::string& entry)
    : library_(library_for_driver(directory, dependencies, entry)),
      display_(library_),
      image_(segments_loaded_from(library_.directory())),
      provided_(call_records().size()) {}

std::int64_t ProviderSession::invoke(std::uint32_t function, std::span<const std::int64_t> words) {
    if (function >= provided_.size()) {
        throw CallError("the call table has no function " + std::to_string(function));
    }
    const std::optional<std::int64_t> answer = invoke_call(function, provided_address(function), words);
    if (!answer.has_value()) {
        const CallRecord& record = call_records()[function];
        throw CallError(
            std::string(record.name) + " takes " + std::to_string(record.word_count) + " arguments, not "
            + std::to_string(words.size()));
    }
    return *answer;
}

void* ProviderSession::provided_address(std::uint32_t function) {
    if (void* known = provided_[function].load(); known != nullptr) {
        return known;
    }
    const std::string name(call_records()[function].name);
    void* address = display_.function_address(name.c_str());
    if (address == nullptr) {
        throw CallError(name + " is not offered by the provider");
    }
    if (!contains_address(image_, reinterpret_cast<std::uintptr_t>(address))) {
        throw CallError(name + " resolved outside the provider library");
    }
    provided_[function].store(address);
    return address;
}

}
