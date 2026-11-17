package io.github.luasgraphicss.minemod.client.compatibility.textures;

import io.github.luasgraphicss.minemod.client.textures.TexelArea;
import io.github.luasgraphicss.minemod.client.textures.TextureShape;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.Objects;

public final class ProviderTextures implements AutoCloseable {
    private static final int GL_MAX_TEXTURE_SIZE = 0x0D33;
    private static final int GL_TEXTURE_2D = 0x0DE1;
    private static final int GL_UNSIGNED_BYTE = 0x1401;
    private static final int GL_RGBA = 0x1908;
    private static final int GL_RGBA8 = 0x8058;
    private static final int GL_TEXTURE_BINDING_2D = 0x8069;
    private static final int ONE = 1;

    private final ProviderCalls provider;
    private final NativeMemory memory;
    private final ScratchInts scratch;
    private final TightUnpacking unpacking;
    private final int maxExtent;

    public ProviderTextures(ProviderCalls provider, NativeMemory memory) {
        this.provider = Objects.requireNonNull(provider, "provider");
        this.memory = Objects.requireNonNull(memory, "memory");
        this.scratch = new ScratchInts(memory, ONE);
        this.unpacking = new TightUnpacking(provider, scratch);
        this.maxExtent = integer(GL_MAX_TEXTURE_SIZE);
    }

    int create() {
        provider.invoke("glGenTextures", new long[] {ONE, scratch.address()});
        return scratch.first();
    }

    boolean holds(TextureShape shape) {
        return shape.width() <= maxExtent && shape.height() <= maxExtent;
    }

    void allocate(int texture, TextureShape shape) {
        whileBound(texture, () -> provider.invoke(
                "glTexStorage2D", new long[] {GL_TEXTURE_2D, shape.levels(), GL_RGBA8, shape.width(), shape.height()}));
    }

    void setParameter(int texture, int parameter, int value) {
        whileBound(texture, () -> provider.invoke("glTexParameteri", new long[] {GL_TEXTURE_2D, parameter, value}));
    }

    void setParameters(int texture, Map<Integer, Integer> parameters) {
        if (parameters.isEmpty()) {
            return;
        }
        whileBound(texture, () -> parameters.forEach((parameter, value) ->
                provider.invoke("glTexParameteri", new long[] {GL_TEXTURE_2D, parameter, value})));
    }

    void write(int texture, TexelArea area, ByteBuffer rgba) {
        int bytes = rgba.remaining();
        long texels = memory.allocate(bytes);
        try {
            memory.writeBytes(texels, rgba.array(), rgba.arrayOffset() + rgba.position(), bytes);
            whileBound(texture, () -> unpacking.around(() -> provider.invoke("glTexSubImage2D", new long[] {
                GL_TEXTURE_2D,
                area.level(),
                area.x(),
                area.y(),
                area.width(),
                area.height(),
                GL_RGBA,
                GL_UNSIGNED_BYTE,
                texels
            })));
        } finally {
            memory.free(texels);
        }
    }

    @Override
    public void close() {
        scratch.close();
    }

    private void whileBound(int texture, Runnable calls) {
        int bound = integer(GL_TEXTURE_BINDING_2D);
        provider.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, texture});
        try {
            calls.run();
        } finally {
            provider.invoke("glBindTexture", new long[] {GL_TEXTURE_2D, bound});
        }
    }

    private int integer(int parameter) {
        provider.invoke("glGetIntegerv", new long[] {parameter, scratch.address()});
        return scratch.first();
    }
}
