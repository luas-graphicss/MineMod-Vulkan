#version 450

layout(location = 0) in vec3 in_position;
layout(location = 1) in vec2 in_texture_coordinate;
layout(location = 2) in vec4 in_tint;

layout(location = 0) out vec2 out_texture_coordinate;
layout(location = 1) flat out vec4 out_tint;

void main() {
    gl_Position = vec4(in_position, 1.0);
    out_texture_coordinate = in_texture_coordinate;
    out_tint = in_tint;
}
