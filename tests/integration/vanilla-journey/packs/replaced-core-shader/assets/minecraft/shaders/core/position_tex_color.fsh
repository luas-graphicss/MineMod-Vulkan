#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 surfaceCoordinate;
in vec4 tint;

out vec4 fragColor;

void main() {
    vec4 color = texture(Sampler0, surfaceCoordinate) * tint * ColorModulator;
    if (color.a < 0.1) {
        discard;
    }
    fragColor = color;
}
