#include "support/command_stream_corpus.hpp"

#include <algorithm>
#include <fstream>
#include <stdexcept>

namespace minemod::abi::testing {
namespace {

constexpr int hexadecimal = 16;
constexpr unsigned long max_byte = 0xFF;

CorpusEntry read_entry(const std::filesystem::path& file) {
    std::ifstream input(file);
    std::string keyword;
    CorpusEntry entry{.name = file.filename().string(), .expected_result = {}, .bytes = {}};
    if (!(input >> keyword >> entry.expected_result) || keyword != "expect") {
        throw std::runtime_error(entry.name + " does not start with 'expect <RESULT>'");
    }
    for (std::string token; input >> token;) {
        std::size_t parsed = 0;
        const unsigned long value = std::stoul(token, &parsed, hexadecimal);
        if (parsed != token.size() || token.size() != 2 || value > max_byte) {
            throw std::runtime_error(entry.name + " holds an invalid byte: " + token);
        }
        entry.bytes.push_back(static_cast<std::uint8_t>(value));
    }
    return entry;
}

}

std::vector<CorpusEntry> load_command_stream_corpus(const std::filesystem::path& directory) {
    std::vector<CorpusEntry> entries;
    for (const auto& file : std::filesystem::directory_iterator(directory)) {
        if (file.path().extension() == ".stream") {
            entries.push_back(read_entry(file.path()));
        }
    }
    std::sort(entries.begin(), entries.end(), [](const CorpusEntry& left, const CorpusEntry& right) {
        return left.name < right.name;
    });
    return entries;
}

}
