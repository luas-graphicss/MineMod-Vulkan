#version 120

uniform sampler2D colortex0;
uniform sampler2D depthtex1;
uniform sampler2D shadowtex0;
uniform mat4 gbufferProjectionInverse;
uniform mat4 gbufferModelViewInverse;
uniform mat4 shadowModelView;
uniform mat4 shadowProjection;

varying vec2 texcoord;

/* DRAWBUFFERS:0 */

void main() {
    vec4 view = gbufferProjectionInverse * vec4(vec3(texcoord, texture2D(depthtex1, texcoord).r) * 2.0 - 1.0, 1.0);
    vec4 shadowClip = shadowProjection * shadowModelView * gbufferModelViewInverse * (view / view.w);
    vec3 shadowCoord = shadowClip.xyz / shadowClip.w * 0.5 + 0.5;
    float lit = step(shadowCoord.z - 0.001, texture2D(shadowtex0, shadowCoord.xy).r);
    gl_FragData[0] = texture2D(colortex0, texcoord) * mix(0.4, 1.0, lit);
}
