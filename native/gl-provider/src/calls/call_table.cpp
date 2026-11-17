#include "calls/call_table.hpp"

#include <algorithm>

namespace minemod::provider {

std::optional<std::uint32_t> find_call(std::string_view name) noexcept {
    const std::span<const CallRecord> records = call_records();
    const auto found = std::lower_bound(
        records.begin(), records.end(), name, [](const CallRecord& record, std::string_view wanted) {
            return record.name < wanted;
        });
    if (found == records.end() || found->name != name) {
        return std::nullopt;
    }
    return static_cast<std::uint32_t>(found - records.begin());
}

std::optional<std::int64_t> invoke_call(std::uint32_t function, void* address, std::span<const std::int64_t> words) {
    const std::span<const CallRecord> records = call_records();
    if (function >= records.size() || address == nullptr || words.size() != records[function].word_count) {
        return std::nullopt;
    }
    return records[function].invoke(address, words);
}

}
