#version 120

uniform sampler2D gtexture;
uniform sampler2D lightmap;

varying vec2 texcoord;
varying vec2 lightcoord;
varying vec3 normal;

/* DRAWBUFFERS:01 */

void main() {
    gl_FragData[0] = texture2D(gtexture, texcoord) * texture2D(lightmap, lightcoord);
    gl_FragData[1] = vec4(normal * 0.5 + 0.5, 1.0);
}
