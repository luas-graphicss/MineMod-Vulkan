#include "support/core_shader_samples.hpp"

#include <string>

namespace minemod::shaders::test_support {

IncludeSources sample_includes() {
    return IncludeSources{{"<shade.glsl>", std::string(shade_include)}};
}

}
