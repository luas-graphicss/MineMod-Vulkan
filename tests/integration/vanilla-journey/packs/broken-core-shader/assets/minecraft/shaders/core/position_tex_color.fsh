#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;

in vec2 surfaceCoordinate;
in vec4 tint;

out vec4 fragColor;

void main() {
    fragColor = texture(Sampler0, surfaceCoordinate) * tint * ColorModulator
}
