package io.github.luasgraphicss.minemod.verification.differential;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.verification.scenario.simulation.GlDriver;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

final class ScriptPlayer {
    private static final int FIRST_UNGENERATED_NAME = 400;
    private static final long GL_TRIANGLES = 0x0004;
    private static final long GL_TEXTURE_2D = 0x0DE1;
    private static final long GL_UNSIGNED_BYTE = 0x1401;
    private static final long GL_RGBA = 0x1908;
    private static final long GL_ARRAY_BUFFER = 0x8892;
    private static final long GL_RENDERBUFFER = 0x8D41;
    private static final long GL_FRAMEBUFFER = 0x8D40;

    private final GlDriver gl;
    private final Map<ObjectKind, List<Integer>> names = new EnumMap<>(ObjectKind.class);
    private final List<String> observations = new ArrayList<>();

    ScriptPlayer(GlDriver gl) {
        this.gl = gl;
    }

    List<String> play(List<ScriptStep> steps) {
        for (ScriptStep step : steps) {
            play(step);
        }
        return observations;
    }

    private void play(ScriptStep step) {
        switch (step.action()) {
            case GENERATE -> {
                int name = gl.generate(step.kind(), 1)[0];
                namesOf(step.kind()).add(name);
                observations.add("generated " + step.kind() + " " + name);
            }
            case BIND -> bind(step.kind(), nameOf(step));
            case BIND_NOTHING -> bind(step.kind(), 0);
            case BIND_UNGENERATED -> bind(step.kind(), FIRST_UNGENERATED_NAME + step.slot());
            case DELETE -> gl.delete(step.kind(), nameOf(step));
            case IS_OBJECT -> observations.add(
                    step.kind() + " " + nameOf(step) + " exists: " + gl.isObject(step.kind(), nameOf(step)));
            case ENABLE -> gl.call("glEnable", step.value());
            case DISABLE -> gl.call("glDisable", step.value());
            case DRAW -> gl.call("glDrawArrays", GL_TRIANGLES, 0, 3);
            case READ -> gl.call("glReadPixels", 0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, 0);
            case IMMEDIATE_TRIANGLE -> drawImmediateTriangle();
            case GET_ERROR -> observations.add("error " + gl.takeError());
        }
    }

    private void bind(ObjectKind kind, int name) {
        switch (kind) {
            case TEXTURE -> gl.call("glBindTexture", GL_TEXTURE_2D, name);
            case BUFFER -> gl.call("glBindBuffer", GL_ARRAY_BUFFER, name);
            case VERTEX_ARRAY -> gl.call("glBindVertexArray", name);
            case SAMPLER -> gl.call("glBindSampler", 0, name);
            case FRAMEBUFFER -> gl.call("glBindFramebuffer", GL_FRAMEBUFFER, name);
            case RENDERBUFFER -> gl.call("glBindRenderbuffer", GL_RENDERBUFFER, name);
            case SHADER_OR_PROGRAM -> gl.call("glUseProgram", name);
            default -> throw new IllegalArgumentException("the script does not bind " + kind);
        }
    }

    private void drawImmediateTriangle() {
        gl.call("glBegin", GL_TRIANGLES);
        for (int vertex = 0; vertex < 3; vertex++) {
            gl.call("glVertex3f", vertex, vertex, 0);
        }
        gl.call("glEnd");
    }

    private int nameOf(ScriptStep step) {
        return namesOf(step.kind()).get(step.slot());
    }

    private List<Integer> namesOf(ObjectKind kind) {
        return names.computeIfAbsent(kind, unused -> new ArrayList<>());
    }
}
