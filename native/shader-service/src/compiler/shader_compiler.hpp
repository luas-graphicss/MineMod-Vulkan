#pragma once

#include <cstdint>
#include <string>
#include <string_view>
#include <vector>

#include "source/stage.hpp"

namespace minemod::shaders {

struct Compilation {
    std::vector<std::uint32_t> spirv;
    std::string log;
};

class ShaderCompiler {
public:
    virtual ~ShaderCompiler() = default;

    virtual Compilation compile(Stage stage, std::string_view source) = 0;
    virtual std::string_view identity() const noexcept = 0;
};

}
