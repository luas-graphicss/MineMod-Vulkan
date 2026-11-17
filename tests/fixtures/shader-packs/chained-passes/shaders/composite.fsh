#version 120

uniform sampler2D colortex0;
uniform sampler2D colortex1;

varying vec2 texcoord;

/* DRAWBUFFERS:2 */

void main() {
    vec3 normal = texture2D(colortex1, texcoord).rgb * 2.0 - 1.0;
    gl_FragData[0] = texture2D(colortex0, texcoord) * (0.75 + 0.25 * normal.y);
}
