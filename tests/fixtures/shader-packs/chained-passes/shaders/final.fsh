#version 120

uniform sampler2D colortex0;

varying vec2 texcoord;

#include "/lib/tonemap.glsl"

void main() {
    gl_FragColor = vec4(tonemap(texture2D(colortex0, texcoord).rgb), 1.0);
}
