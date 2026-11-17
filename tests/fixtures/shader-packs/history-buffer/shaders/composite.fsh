#version 120

uniform sampler2D colortex0;
uniform sampler2D colortex3;

varying vec2 texcoord;

/*
const int colortex3Format = RGBA16F;
const bool colortex3Clear = false;
*/

/* DRAWBUFFERS:3 */

void main() {
    gl_FragData[0] = mix(texture2D(colortex3, texcoord), texture2D(colortex0, texcoord), 0.1);
}
