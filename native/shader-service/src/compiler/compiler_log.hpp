#pragma once

#include <span>
#include <string>
#include <string_view>
#include <vector>

namespace minemod::shaders {

std::vector<std::string> located_errors(std::string_view log, std::span<const std::string> files);

}
