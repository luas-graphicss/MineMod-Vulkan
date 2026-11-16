#pragma once

#include <cstddef>
#include <span>
#include <string_view>
#include <vector>

#include "syntax/glsl_tokens.hpp"

namespace minemod::shaders {

enum class InterfaceKind { input, output, uniform };

struct InterfaceDeclaration {
    InterfaceKind kind = InterfaceKind::input;
    std::string_view interpolation;
    std::string_view type;
    std::vector<std::string_view> names;
    bool is_array = false;
    bool has_layout = false;
    bool is_block = false;
    bool has_initializer = false;
    std::size_t first_token = 0;
    std::size_t last_token = 0;
};

std::vector<InterfaceDeclaration> parse_interface(std::span<const Token> tokens);

}
