#include "adaptation/core_shader_bindings.hpp"

#include <array>

namespace minemod::shaders {

namespace {

constexpr std::string_view scene_constant_blocks =
    "layout(push_constant) uniform MinemodDraw {\n"
    "    mat4 model_view;\n"
    "    vec4 color_modulator;\n"
    "    float alpha_cutoff;\n"
    "    uint shading;\n"
    "    vec2 frame_size;\n"
    "    float line_width;\n"
    "    float texture_rows[6];\n"
    "} minemod_draw;\n"
    "layout(set = 0, binding = 0) uniform MinemodView {\n"
    "    mat4 projection;\n"
    "    vec4 fog_color;\n"
    "    vec4 light_directions[2];\n"
    "    float fog_start;\n"
    "    float fog_end;\n"
    "    uint fog_shape;\n"
    "} minemod_view;\n";

constexpr std::array scene_attributes{
    AttributeBinding{"Position", "vec3", 0},
    AttributeBinding{"Color", "vec4", 1},
    AttributeBinding{"UV0", "vec2", 2},
    AttributeBinding{"UV1", "ivec2", 3},
    AttributeBinding{"UV2", "ivec2", 4},
    AttributeBinding{"Normal", "vec3", 5},
};

constexpr std::array scene_uniforms{
    UniformBinding{"ModelViewMat", "mat4", "minemod_draw.model_view"},
    UniformBinding{"ProjMat", "mat4", "minemod_view.projection"},
    UniformBinding{"ColorModulator", "vec4", "minemod_draw.color_modulator"},
    UniformBinding{"FogStart", "float", "minemod_view.fog_start"},
    UniformBinding{"FogEnd", "float", "minemod_view.fog_end"},
    UniformBinding{"FogColor", "vec4", "minemod_view.fog_color"},
    UniformBinding{"FogShape", "int", "int(minemod_view.fog_shape)"},
    UniformBinding{"Light0_Direction", "vec3", "minemod_view.light_directions[0].xyz"},
    UniformBinding{"Light1_Direction", "vec3", "minemod_view.light_directions[1].xyz"},
    UniformBinding{"ScreenSize", "vec2", "minemod_draw.frame_size"},
    UniformBinding{"LineWidth", "float", "minemod_draw.line_width"},
    UniformBinding{
        "TextureMat",
        "mat4",
        "mat4(vec4(minemod_draw.texture_rows[0], minemod_draw.texture_rows[3], 0.0, 0.0), "
        "vec4(minemod_draw.texture_rows[1], minemod_draw.texture_rows[4], 0.0, 0.0), "
        "vec4(0.0, 0.0, 1.0, 0.0), "
        "vec4(minemod_draw.texture_rows[2], minemod_draw.texture_rows[5], 0.0, 1.0))",
    },
};

constexpr std::array scene_samplers{
    SamplerBinding{"Sampler0", Stage::fragment, 1, 0},
    SamplerBinding{"Sampler1", Stage::vertex, 0, 2},
    SamplerBinding{"Sampler2", Stage::vertex, 0, 1},
};

constexpr CoreShaderBindings scene{
    "scene", 1, scene_constant_blocks, scene_attributes, scene_uniforms, scene_samplers,
};

}

const CoreShaderBindings& scene_bindings() noexcept {
    return scene;
}

}
