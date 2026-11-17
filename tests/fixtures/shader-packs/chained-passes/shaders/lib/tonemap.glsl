uniform float screenBrightness;

vec3 tonemap(vec3 color) {
    return color / (color + vec3(1.0)) * (0.8 + 0.4 * screenBrightness);
}
