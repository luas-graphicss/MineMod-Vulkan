#version 450

layout(set = 0, binding = 0) uniform sampler2D surface_texture;

layout(location = 0) in vec2 in_texture_coordinate;
layout(location = 1) flat in vec4 in_tint;

layout(location = 0) out vec4 out_color;

void main() {
    vec4 texel = texture(surface_texture, in_texture_coordinate);
    out_color = vec4(mix(texel.rgb, in_tint.rgb, in_tint.a), 1.0);
}
