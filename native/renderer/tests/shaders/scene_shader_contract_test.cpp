#include <stdint.h>

#include <doctest.h>

#include "scene_frag.h"
#include "scene_vert.h"
#include "shaders/core_shader_contract.hpp"
#include "shaders/shader_interface.hpp"
#include "shaders/spirv_module.hpp"

namespace {

using namespace minemod::renderer;

}

TEST_CASE("the scene shaders read the six vertex elements, the view set and one surface texture") {
    const CoreShaderContract contract{"scene", {0, 1, 2, 3, 4, 5}, {{0, 0}, {0, 1}, {0, 2}, {1, 0}}, {0}};

    const ShaderInterface vertex = reflect_interface(SpirvModule{scene_vert});
    const ShaderInterface fragment = reflect_interface(SpirvModule{scene_frag});

    CHECK(contract_violations(contract, vertex, fragment).empty());
    CHECK(vertex.input_locations.size() == contract.vertex_locations.size());
}
