#version 450

layout(push_constant) uniform Draw {
    mat4 model_view;
    vec4 color_modulator;
    float alpha_cutoff;
    uint shading;
    vec2 frame_size;
    float line_width;
} draw;

layout(set = 0, binding = 0) uniform View {
    mat4 projection;
    vec4 fog_color;
    vec4 light_directions[2];
    float fog_start;
    float fog_end;
    uint fog_shape;
} view;

layout(set = 0, binding = 1) uniform sampler2D lightmap;
layout(set = 0, binding = 2) uniform sampler2D overlay;

layout(location = 0) in vec3 in_position;
layout(location = 1) in vec4 in_color;
layout(location = 2) in vec2 in_texture_coordinate;
layout(location = 3) in ivec2 in_overlay_texel;
layout(location = 4) in ivec2 in_packed_light;
layout(location = 5) in vec3 in_normal;

layout(location = 0) out vec4 out_color;
layout(location = 1) out vec2 out_texture_coordinate;
layout(location = 2) out vec4 out_overlay_color;
layout(location = 3) out vec4 out_light_color;
layout(location = 4) out float out_fog_distance;

const uint applies_lightmap = 2u;
const uint applies_overlay = 4u;
const uint shades_by_normal = 8u;
const uint widens_lines = 32u;
const uint cylindrical_fog = 1u;

const float directional_light_strength = 0.6;
const float ambient_light = 0.4;
const int packed_light_per_texel = 16;
const ivec2 first_table_texel = ivec2(0);
const ivec2 last_table_texel = ivec2(15);
const vec4 untouched_by_overlay = vec4(0.0, 0.0, 0.0, 1.0);

float diffuse_light(vec3 normal) {
    float first = max(0.0, dot(view.light_directions[0].xyz, normal));
    float second = max(0.0, dot(view.light_directions[1].xyz, normal));
    return min(1.0, (first + second) * directional_light_strength + ambient_light);
}

float fog_distance(vec3 from_camera) {
    if (view.fog_shape == cylindrical_fog) {
        return max(length(from_camera.xz), abs(from_camera.y));
    }
    return length(from_camera);
}

vec4 table_texel(sampler2D table, ivec2 texel) {
    return texelFetch(table, clamp(texel, first_table_texel, last_table_texel), 0);
}

vec4 widened_line_corner(vec4 start, vec4 toward_end) {
    vec3 start_point = start.xyz / start.w;
    vec3 end_point = toward_end.xyz / toward_end.w;
    vec2 direction = normalize((end_point.xy - start_point.xy) * draw.frame_size);
    vec2 offset = vec2(-direction.y, direction.x) * draw.line_width / draw.frame_size;
    if (offset.x < 0.0) {
        offset = -offset;
    }
    float side = gl_VertexIndex % 2 == 0 ? 1.0 : -1.0;
    return vec4((start_point + vec3(offset * side, 0.0)) * start.w, start.w);
}

void main() {
    mat4 transform = view.projection * draw.model_view;
    gl_Position = transform * vec4(in_position, 1.0);
    if ((draw.shading & widens_lines) != 0u) {
        gl_Position = widened_line_corner(gl_Position, transform * vec4(in_position + in_normal, 1.0));
    }

    out_color = in_color;
    if ((draw.shading & shades_by_normal) != 0u) {
        out_color.rgb *= diffuse_light(in_normal);
    }
    out_texture_coordinate = in_texture_coordinate;
    out_overlay_color =
        (draw.shading & applies_overlay) != 0u ? table_texel(overlay, in_overlay_texel) : untouched_by_overlay;
    out_light_color = (draw.shading & applies_lightmap) != 0u
        ? table_texel(lightmap, in_packed_light / packed_light_per_texel)
        : vec4(1.0);
    out_fog_distance = fog_distance(in_position);
}
