#version 450

layout(push_constant) uniform Frame {
    mat4 view_projection;
    vec4 fog_color;
    vec3 section_origin;
    float alpha_cutoff;
    float fog_start;
    float fog_end;
    uint samples_base_level_only;
    uint fog_shape;
} frame;

layout(set = 0, binding = 0) uniform sampler2D block_atlas;
layout(set = 0, binding = 1) uniform sampler2D lightmap;

layout(location = 0) in vec4 in_color;
layout(location = 1) in vec2 in_texture_coordinate;
layout(location = 2) in vec2 in_light_coordinate;
layout(location = 3) in float in_fog_distance;

layout(location = 0) out vec4 out_color;

vec4 sample_atlas() {
    if (frame.samples_base_level_only != 0u) {
        return textureLod(block_atlas, in_texture_coordinate, 0.0);
    }
    return texture(block_atlas, in_texture_coordinate);
}

void main() {
    vec4 color = sample_atlas() * in_color;
    if (color.a < frame.alpha_cutoff) {
        discard;
    }

    vec3 lit = color.rgb * texture(lightmap, in_light_coordinate).rgb;
    float fog = smoothstep(frame.fog_start, frame.fog_end, in_fog_distance) * frame.fog_color.a;
    out_color = vec4(mix(lit, frame.fog_color.rgb, fog), color.a);
}
