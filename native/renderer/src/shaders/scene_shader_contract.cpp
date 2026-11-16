#include "shaders/scene_shader_contract.hpp"

namespace minemod::renderer {

const CoreShaderContract& scene_shader_contract() {
    static const CoreShaderContract contract{
        "scene",
        {0, 1, 2, 3, 4, 5},
        {{0, 0}, {0, 1}, {0, 2}, {1, 0}},
        {0},
    };
    return contract;
}

}
