#version 450

layout(set = 0, binding = 0) uniform sampler2D frame;

layout(push_constant) uniform BlurConstants {
    vec2 sample_step;
    float radius;
} blur;

layout(location = 0) in vec2 in_frame_coordinate;

layout(location = 0) out vec4 out_color;

void main() {
    vec4 blurred = vec4(0.0);
    for (float offset = 0.5 - blur.radius; offset <= blur.radius; offset += 2.0) {
        blurred += texture(frame, in_frame_coordinate + blur.sample_step * offset);
    }
    blurred += texture(frame, in_frame_coordinate + blur.sample_step * blur.radius) / 2.0;
    out_color = blurred / (blur.radius + 0.5);
}
