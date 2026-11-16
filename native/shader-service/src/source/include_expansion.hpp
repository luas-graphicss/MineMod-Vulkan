#pragma once

#include <cstddef>
#include <functional>
#include <map>
#include <string>
#include <string_view>
#include <vector>

namespace minemod::shaders {

struct SourceLimits {
    std::size_t max_file_bytes;
    std::size_t max_expanded_bytes;
    std::size_t max_include_depth;
    std::size_t max_includes;
};

inline constexpr SourceLimits default_source_limits{std::size_t{1} << 18, std::size_t{1} << 20, 8, 64};

using IncludeSources = std::map<std::string, std::string, std::less<>>;

struct ExpandedSource {
    std::string text;
    std::vector<std::string> files;
};

ExpandedSource expand_includes(
    std::string_view name,
    std::string_view text,
    const IncludeSources& includes,
    const SourceLimits& limits = default_source_limits);

}
