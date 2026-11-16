#version 450

layout(push_constant) uniform Draw {
    mat4 model_view;
    vec4 color_modulator;
    float alpha_cutoff;
    uint shading;
} draw;

layout(set = 0, binding = 0) uniform View {
    mat4 projection;
    vec4 fog_color;
    vec4 light_directions[2];
    float fog_start;
    float fog_end;
    uint fog_shape;
} view;

layout(set = 1, binding = 0) uniform sampler2D surface_texture;

layout(location = 0) in vec4 in_color;
layout(location = 1) in vec2 in_texture_coordinate;
layout(location = 2) in vec4 in_overlay_color;
layout(location = 3) in vec4 in_light_color;
layout(location = 4) in float in_fog_distance;

layout(location = 0) out vec4 out_color;

const uint samples_texture = 1u;
const uint fades_into_fog = 16u;

void main() {
    vec4 color = in_color * draw.color_modulator;
    if ((draw.shading & samples_texture) != 0u) {
        color *= texture(surface_texture, in_texture_coordinate);
    }
    if (color.a < draw.alpha_cutoff) {
        discard;
    }

    color.rgb = mix(in_overlay_color.rgb, color.rgb, in_overlay_color.a);
    color *= in_light_color;
    if ((draw.shading & fades_into_fog) != 0u) {
        float fog = smoothstep(view.fog_start, view.fog_end, in_fog_distance) * view.fog_color.a;
        color.rgb = mix(color.rgb, view.fog_color.rgb, fog);
    }
    out_color = color;
}
