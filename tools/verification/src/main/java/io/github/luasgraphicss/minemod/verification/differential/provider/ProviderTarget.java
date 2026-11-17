package io.github.luasgraphicss.minemod.verification.differential.provider;

import io.github.luasgraphicss.minemod.compatibility.contracts.object.ObjectKind;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.verification.differential.ScriptTarget;
import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;

final class ProviderTarget implements ScriptTarget, AutoCloseable {
    private static final int NO_NAME = 0;
    private static final long GL_FALSE = 0;
    private static final long GL_UNSIGNED_BYTE = 0x1401;
    private static final long GL_RGBA = 0x1908;
    private static final int UNWRITTEN_TEXEL = 0x5A5A5A5A;
    private static final int CHANNELS = 4;
    private static final int CHANNEL_MASK = 0xFF;
    private static final int NAMES_BETWEEN_UNUSED_NAMES = 500;
    private static final Map<ObjectKind, String> NOUNS = new EnumMap<>(ObjectKind.class);

    static {
        NOUNS.put(ObjectKind.TEXTURE, "Texture");
        NOUNS.put(ObjectKind.BUFFER, "Buffer");
        NOUNS.put(ObjectKind.VERTEX_ARRAY, "VertexArray");
        NOUNS.put(ObjectKind.SAMPLER, "Sampler");
        NOUNS.put(ObjectKind.FRAMEBUFFER, "Framebuffer");
        NOUNS.put(ObjectKind.RENDERBUFFER, "Renderbuffer");
        NOUNS.put(ObjectKind.SHADER_OR_PROGRAM, "Program");
    }

    private final GlCalls gl;
    private final NativeMemory memory;
    private final long word;

    ProviderTarget(GlCalls gl, NativeMemory memory) {
        this.gl = gl;
        this.memory = memory;
        this.word = memory.allocate(Integer.BYTES);
    }

    @Override
    public int generate(ObjectKind kind) {
        if (kind == ObjectKind.SHADER_OR_PROGRAM) {
            return (int) gl.call("glCreateProgram");
        }
        memory.writeInt(word, NO_NAME);
        gl.call("glGen" + nounOf(kind) + "s", 1, word);
        return memory.readInt(word);
    }

    @Override
    public void delete(ObjectKind kind, int name) {
        if (kind == ObjectKind.SHADER_OR_PROGRAM) {
            gl.call("glDeleteProgram", name);
            return;
        }
        memory.writeInt(word, name);
        gl.call("glDelete" + nounOf(kind) + "s", 1, word);
    }

    @Override
    public boolean isObject(ObjectKind kind, int name) {
        return gl.call("glIs" + nounOf(kind), name) != GL_FALSE;
    }

    @Override
    public void call(String function, long... arguments) {
        gl.call(function, arguments);
    }

    @Override
    public Optional<String> readPixel() {
        memory.writeInt(word, UNWRITTEN_TEXEL);
        gl.call("glReadPixels", 0, 0, 1, 1, GL_RGBA, GL_UNSIGNED_BYTE, word);
        if (memory.readInt(word) == UNWRITTEN_TEXEL) {
            return Optional.of("nothing");
        }
        StringBuilder texel = new StringBuilder();
        for (int channel = 0; channel < CHANNELS; channel++) {
            texel.append(String.format("%02x", memory.readByte(word + channel) & CHANNEL_MASK));
        }
        return Optional.of(texel.toString());
    }

    @Override
    public int takeError() {
        return (int) gl.call("glGetError");
    }

    @Override
    public String observedName(int slot, int name) {
        return name == NO_NAME ? "#" + slot + " without a name" : "#" + slot;
    }

    @Override
    public int unusedName(int index) {
        return NAMES_BETWEEN_UNUSED_NAMES * (index + 1);
    }

    @Override
    public void close() {
        memory.free(word);
    }

    private static String nounOf(ObjectKind kind) {
        String noun = NOUNS.get(kind);
        if (noun == null) {
            throw new IllegalArgumentException("the script holds no " + kind);
        }
        return noun;
    }
}
