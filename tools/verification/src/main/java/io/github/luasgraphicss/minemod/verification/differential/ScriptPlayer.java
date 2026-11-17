package io.github.luasgraphicss.minemod.verification.differential;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class ScriptPlayer {
    private static final long GL_TRIANGLES = 0x0004;
    private static final long GL_TEXTURE_2D = 0x0DE1;
    private static final long GL_ARRAY_BUFFER = 0x8892;
    private static final long GL_RENDERBUFFER = 0x8D41;
    private static final long GL_FRAMEBUFFER = 0x8D40;
    private static final long LOW_WORD = 0xFFFFFFFFL;
    private static final float[][] VIEWPORT_COVERING_TRIANGLE = {{-1, -1}, {3, -1}, {-1, 3}};

    private final ScriptTarget target;
    private final Map<ObjectKind, List<Integer>> names = new EnumMap<>(ObjectKind.class);
    private final List<String> observations = new ArrayList<>();

    public ScriptPlayer(ScriptTarget target) {
        this.target = target;
    }

    public List<String> play(List<ScriptStep> steps) {
        for (ScriptStep step : steps) {
            play(step);
        }
        return observations;
    }

    private void play(ScriptStep step) {
        switch (step.action()) {
            case GENERATE -> generate(step.kind());
            case BIND -> bind(step.kind(), nameOf(step));
            case BIND_NOTHING -> bind(step.kind(), 0);
            case BIND_UNGENERATED -> bind(step.kind(), target.unusedName(step.slot()));
            case DELETE -> delete(step.kind(), step.slot());
            case IS_OBJECT -> observations.add(step.kind() + " " + target.observedName(step.slot(), nameOf(step))
                    + " exists: " + target.isObject(step.kind(), nameOf(step)));
            case ENABLE -> target.call("glEnable", step.value());
            case DISABLE -> target.call("glDisable", step.value());
            case DRAW -> target.call("glDrawArrays", GL_TRIANGLES, 0, 3);
            case READ -> target.readPixel().ifPresent(pixel -> observations.add("read " + pixel));
            case IMMEDIATE_TRIANGLE -> drawImmediateTriangle();
            case GET_ERROR -> observations.add("error " + target.takeError());
        }
    }

    private void generate(ObjectKind kind) {
        List<Integer> generated = namesOf(kind);
        int name = target.generate(kind);
        observations.add("generated " + kind + " " + target.observedName(generated.size(), name));
        generated.add(name);
    }

    private void delete(ObjectKind kind, int slot) {
        List<Integer> generated = namesOf(kind);
        target.delete(kind, generated.get(slot));
        generated.set(slot, target.unusedName(ScriptGenerator.UNGENERATED_NAMES + slot));
    }

    private void bind(ObjectKind kind, int name) {
        switch (kind) {
            case TEXTURE -> target.call("glBindTexture", GL_TEXTURE_2D, name);
            case BUFFER -> target.call("glBindBuffer", GL_ARRAY_BUFFER, name);
            case VERTEX_ARRAY -> target.call("glBindVertexArray", name);
            case SAMPLER -> target.call("glBindSampler", 0, name);
            case FRAMEBUFFER -> target.call("glBindFramebuffer", GL_FRAMEBUFFER, name);
            case RENDERBUFFER -> target.call("glBindRenderbuffer", GL_RENDERBUFFER, name);
            case SHADER_OR_PROGRAM -> target.call("glUseProgram", name);
            default -> throw new IllegalArgumentException("the script does not bind " + kind);
        }
    }

    private void drawImmediateTriangle() {
        target.call("glBegin", GL_TRIANGLES);
        for (float[] vertex : VIEWPORT_COVERING_TRIANGLE) {
            target.call("glVertex3f", wordOf(vertex[0]), wordOf(vertex[1]), wordOf(0));
        }
        target.call("glEnd");
    }

    private int nameOf(ScriptStep step) {
        return namesOf(step.kind()).get(step.slot());
    }

    private List<Integer> namesOf(ObjectKind kind) {
        return names.computeIfAbsent(kind, unused -> new ArrayList<>());
    }

    private static long wordOf(float value) {
        return Float.floatToRawIntBits(value) & LOW_WORD;
    }
}
