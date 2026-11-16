#pragma once

#include <string_view>

namespace minemod::shaders {

enum class Stage { vertex, fragment };

constexpr std::string_view stage_name(Stage stage) noexcept {
    return stage == Stage::vertex ? "vertex" : "fragment";
}

}
