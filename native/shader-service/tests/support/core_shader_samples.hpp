#pragma once

#include <string_view>

#include "source/include_expansion.hpp"

namespace minemod::shaders::test_support {

inline constexpr std::string_view tinted_texture_vertex =
    "#version 150\n"
    "\n"
    "in vec3 Position;\n"
    "in vec2 UV0;\n"
    "in vec4 Color;\n"
    "\n"
    "uniform mat4 ModelViewMat;\n"
    "uniform mat4 ProjMat;\n"
    "\n"
    "out vec2 surfaceCoordinate;\n"
    "out vec4 tint;\n"
    "\n"
    "void main() {\n"
    "    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n"
    "    surfaceCoordinate = UV0;\n"
    "    tint = Color;\n"
    "}\n";

inline constexpr std::string_view tinted_texture_fragment =
    "#version 150\n"
    "\n"
    "uniform sampler2D Sampler0;\n"
    "uniform vec4 ColorModulator;\n"
    "\n"
    "in vec2 surfaceCoordinate;\n"
    "in vec4 tint;\n"
    "\n"
    "out vec4 fragColor;\n"
    "\n"
    "void main() {\n"
    "    vec4 color = texture(Sampler0, surfaceCoordinate) * tint * ColorModulator;\n"
    "    if (color.a < 0.1) {\n"
    "        discard;\n"
    "    }\n"
    "    fragColor = color;\n"
    "}\n";

inline constexpr std::string_view lit_entity_vertex =
    "#version 150\n"
    "\n"
    "#moj_import <shade.glsl>\n"
    "\n"
    "in vec3 Position;\n"
    "in vec4 Color;\n"
    "in vec2 UV0;\n"
    "in ivec2 UV1;\n"
    "in ivec2 UV2;\n"
    "in vec3 Normal;\n"
    "\n"
    "uniform sampler2D Sampler1;\n"
    "uniform sampler2D Sampler2;\n"
    "uniform mat4 ModelViewMat;\n"
    "uniform mat4 ProjMat;\n"
    "uniform mat4 TextureMat;\n"
    "uniform int FogShape;\n"
    "uniform vec3 Light0_Direction;\n"
    "uniform vec3 Light1_Direction;\n"
    "\n"
    "out float eyeDistance;\n"
    "out vec4 shadedColor;\n"
    "out vec4 lightColor;\n"
    "out vec4 overlayColor;\n"
    "out vec2 surfaceCoordinate;\n"
    "flat out int vertexParity;\n"
    "\n"
    "void main() {\n"
    "    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);\n"
    "    eyeDistance = FogShape == 1 ? max(length(Position.xz), abs(Position.y)) : length(Position);\n"
    "    shadedColor = shaded(Light0_Direction, Light1_Direction, Normal, Color);\n"
    "    lightColor = texelFetch(Sampler2, UV2 / 16, 0);\n"
    "    overlayColor = texelFetch(Sampler1, UV1, 0);\n"
    "    surfaceCoordinate = (TextureMat * vec4(UV0, 0.0, 1.0)).xy;\n"
    "    vertexParity = gl_VertexID % 2;\n"
    "}\n";

inline constexpr std::string_view lit_entity_fragment =
    "#version 150\n"
    "\n"
    "uniform sampler2D Sampler0;\n"
    "uniform vec4 ColorModulator;\n"
    "uniform float FogStart;\n"
    "uniform float FogEnd;\n"
    "uniform vec4 FogColor;\n"
    "uniform float GameTime;\n"
    "\n"
    "in float eyeDistance;\n"
    "in vec4 shadedColor;\n"
    "in vec4 lightColor;\n"
    "in vec4 overlayColor;\n"
    "in vec2 surfaceCoordinate;\n"
    "flat in int vertexParity;\n"
    "\n"
    "out vec4 fragColor;\n"
    "\n"
    "void main() {\n"
    "    vec4 color = texture(Sampler0, surfaceCoordinate) * shadedColor * ColorModulator;\n"
    "    color.rgb = mix(overlayColor.rgb, color.rgb, overlayColor.a);\n"
    "    color *= lightColor;\n"
    "    float fog = smoothstep(FogStart, FogEnd, eyeDistance) * FogColor.a;\n"
    "    color.rgb = mix(color.rgb, FogColor.rgb, fog);\n"
    "    fragColor = vertexParity == 0 ? color : color * 0.99;\n"
    "}\n";

inline constexpr std::string_view shade_include =
    "#version 150\n"
    "\n"
    "vec4 shaded(vec3 firstLight, vec3 secondLight, vec3 normal, vec4 color) {\n"
    "    float first = max(0.0, dot(firstLight, normal));\n"
    "    float second = max(0.0, dot(secondLight, normal));\n"
    "    float strength = min(1.0, (first + second) * 0.6 + 0.4);\n"
    "    return vec4(color.rgb * strength, color.a);\n"
    "}\n";

IncludeSources sample_includes();

}
