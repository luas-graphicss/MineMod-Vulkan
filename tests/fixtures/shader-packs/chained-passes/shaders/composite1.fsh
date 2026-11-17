#version 120

uniform sampler2D colortex2;
uniform sampler2D depthtex0;
uniform float frameTimeCounter;
uniform float far;

varying vec2 texcoord;

/* DRAWBUFFERS:0 */

void main() {
    float fog = smoothstep(0.9, 1.0, texture2D(depthtex0, texcoord).r) * (0.5 + 0.5 * sin(frameTimeCounter / far));
    gl_FragData[0] = mix(texture2D(colortex2, texcoord), vec4(0.7, 0.8, 1.0, 1.0), fog);
}
