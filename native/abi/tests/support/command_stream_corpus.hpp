#pragma once

#include <cstdint>
#include <filesystem>
#include <string>
#include <vector>

namespace minemod::abi::testing {

struct CorpusEntry {
    std::string name;
    std::string expected_result;
    std::vector<std::uint8_t> bytes;
};

std::vector<CorpusEntry> load_command_stream_corpus(const std::filesystem::path& directory);

}
