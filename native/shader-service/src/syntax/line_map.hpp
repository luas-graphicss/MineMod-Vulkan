#pragma once

#include <cstddef>
#include <span>
#include <string>
#include <vector>

#include "syntax/glsl_tokens.hpp"

namespace minemod::shaders {

struct SourceOrigin {
    std::size_t file;
    std::size_t line;
};

class LineMap {
public:
    LineMap() noexcept = default;
    explicit LineMap(std::span<const Token> tokens);

    SourceOrigin origin_of(std::size_t physical_line) const noexcept;

private:
    struct Marker {
        std::size_t physical_line;
        SourceOrigin following;
    };

    std::vector<Marker> markers_;
};

std::string describe(const SourceOrigin& origin, std::span<const std::string> files);

}
