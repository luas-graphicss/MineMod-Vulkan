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

layout(location = 0) in vec3 in_position;
layout(location = 1) in vec4 in_color;
layout(location = 2) in vec2 in_texture_coordinate;
layout(location = 3) in uvec2 in_packed_light;

layout(location = 0) out vec4 out_color;
layout(location = 1) out vec2 out_texture_coordinate;
layout(location = 2) out vec2 out_light_coordinate;
layout(location = 3) out float out_fog_distance;

const vec2 first_light_texel = vec2(0.5 / 16.0);
const vec2 last_light_texel = vec2(15.5 / 16.0);
const uint cylindrical_fog = 1u;

float fog_distance(vec3 from_camera) {
    if (frame.fog_shape == cylindrical_fog) {
        return max(length(from_camera.xz), abs(from_camera.y));
    }
    return length(from_camera);
}

void main() {
    vec3 camera_relative = in_position + frame.section_origin;
    gl_Position = frame.view_projection * vec4(camera_relative, 1.0);
    out_color = in_color;
    out_texture_coordinate = in_texture_coordinate;
    out_light_coordinate = clamp(vec2(in_packed_light) / 256.0, first_light_texel, last_light_texel);
    out_fog_distance = fog_distance(camera_relative);
}
