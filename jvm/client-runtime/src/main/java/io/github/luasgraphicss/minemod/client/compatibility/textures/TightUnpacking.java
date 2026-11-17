package io.github.luasgraphicss.minemod.client.compatibility.textures;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;

final class TightUnpacking {
    private static final int GL_UNPACK_ROW_LENGTH = 0x0CF2;
    private static final int GL_UNPACK_SKIP_ROWS = 0x0CF3;
    private static final int GL_UNPACK_SKIP_PIXELS = 0x0CF4;
    private static final int GL_UNPACK_ALIGNMENT = 0x0CF5;
    private static final int GL_PIXEL_UNPACK_BUFFER = 0x88EC;
    private static final int GL_PIXEL_UNPACK_BUFFER_BINDING = 0x88EF;
    private static final int[] PARAMETERS = {
        GL_UNPACK_ROW_LENGTH, GL_UNPACK_SKIP_ROWS, GL_UNPACK_SKIP_PIXELS, GL_UNPACK_ALIGNMENT
    };
    private static final int[] ROWS_OF_RGBA_BYTES = {0, 0, 0, 4};
    private static final int NO_BUFFER = 0;

    private final ProviderCalls provider;
    private final ScratchInts scratch;

    TightUnpacking(ProviderCalls provider, ScratchInts scratch) {
        this.provider = provider;
        this.scratch = scratch;
    }

    void around(Runnable upload) {
        int[] found = new int[PARAMETERS.length];
        for (int index = 0; index < PARAMETERS.length; index++) {
            found[index] = integer(PARAMETERS[index]);
            store(index, found[index], ROWS_OF_RGBA_BYTES[index]);
        }
        int unpackBuffer = integer(GL_PIXEL_UNPACK_BUFFER_BINDING);
        bindUnpackBuffer(unpackBuffer, NO_BUFFER);
        try {
            upload.run();
        } finally {
            bindUnpackBuffer(NO_BUFFER, unpackBuffer);
            for (int index = 0; index < PARAMETERS.length; index++) {
                store(index, ROWS_OF_RGBA_BYTES[index], found[index]);
            }
        }
    }

    private void store(int index, int current, int wanted) {
        if (current != wanted) {
            provider.invoke("glPixelStorei", new long[] {PARAMETERS[index], wanted});
        }
    }

    private void bindUnpackBuffer(int current, int wanted) {
        if (current != wanted) {
            provider.invoke("glBindBuffer", new long[] {GL_PIXEL_UNPACK_BUFFER, wanted});
        }
    }

    private int integer(int parameter) {
        provider.invoke("glGetIntegerv", new long[] {parameter, scratch.address()});
        return scratch.first();
    }
}
