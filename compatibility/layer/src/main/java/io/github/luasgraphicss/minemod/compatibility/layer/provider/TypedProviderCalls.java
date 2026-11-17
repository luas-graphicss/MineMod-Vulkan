package io.github.luasgraphicss.minemod.compatibility.layer.provider;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.state.error.ProviderErrors;
import io.github.luasgraphicss.minemod.compatibility.translation.legacy.ProviderLists;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderObjects;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class TypedProviderCalls implements ProviderObjects, ProviderLists, ProviderErrors {
    private static final long[] NO_ARGUMENTS = new long[0];
    private static final int ONE = 1;
    private static final int CREATES_PROGRAM = 0;
    private static final Map<ObjectKind, String> GENERATORS = new EnumMap<>(ObjectKind.class);
    private static final Map<ObjectKind, String> DELETERS = new EnumMap<>(ObjectKind.class);

    static {
        pair(ObjectKind.TEXTURE, "Textures");
        pair(ObjectKind.BUFFER, "Buffers");
        pair(ObjectKind.RENDERBUFFER, "Renderbuffers");
        pair(ObjectKind.SAMPLER, "Samplers");
        pair(ObjectKind.FRAMEBUFFER, "Framebuffers");
        pair(ObjectKind.VERTEX_ARRAY, "VertexArrays");
        pair(ObjectKind.QUERY, "Queries");
        pair(ObjectKind.TRANSFORM_FEEDBACK, "TransformFeedbacks");
        pair(ObjectKind.PROGRAM_PIPELINE, "ProgramPipelines");
    }

    private final ProviderCalls calls;
    private final ScratchInts scratch;

    public TypedProviderCalls(ProviderCalls calls, ScratchInts scratch) {
        this.calls = Objects.requireNonNull(calls, "calls");
        this.scratch = Objects.requireNonNull(scratch, "scratch");
    }

    @Override
    public int create(ObjectKind kind, int parameter) {
        if (kind == ObjectKind.DISPLAY_LIST) {
            return generate(ONE);
        }
        if (kind == ObjectKind.SHADER_OR_PROGRAM) {
            return parameter == CREATES_PROGRAM
                    ? (int) calls.invoke("glCreateProgram", NO_ARGUMENTS)
                    : (int) calls.invoke("glCreateShader", new long[] {parameter});
        }
        calls.invoke(GENERATORS.get(kind), new long[] {ONE, scratch.address()});
        return scratch.first();
    }

    @Override
    public void dispose(ObjectKind kind, int backing) {
        if (kind == ObjectKind.DISPLAY_LIST) {
            delete(backing, ONE);
        } else if (kind == ObjectKind.SHADER_OR_PROGRAM) {
            boolean isProgram = calls.invoke("glIsProgram", new long[] {backing}) != 0;
            calls.invoke(isProgram ? "glDeleteProgram" : "glDeleteShader", new long[] {backing});
        } else {
            calls.invoke(DELETERS.get(kind), new long[] {ONE, scratch.holding(backing)});
        }
    }

    @Override
    public int generate(int range) {
        return (int) calls.invoke("glGenLists", new long[] {range});
    }

    @Override
    public void delete(int firstBacking, int range) {
        calls.invoke("glDeleteLists", new long[] {firstBacking, range});
    }

    @Override
    public int takeError() {
        return (int) calls.invoke("glGetError", NO_ARGUMENTS);
    }

    private static void pair(ObjectKind kind, String plural) {
        GENERATORS.put(kind, "glGen" + plural);
        DELETERS.put(kind, "glDelete" + plural);
    }
}
