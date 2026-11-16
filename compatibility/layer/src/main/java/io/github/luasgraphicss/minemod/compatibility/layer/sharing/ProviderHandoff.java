package io.github.luasgraphicss.minemod.compatibility.layer.sharing;

import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeInts;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.NativeMemory;
import io.github.luasgraphicss.minemod.compatibility.layer.memory.ScratchInts;
import io.github.luasgraphicss.minemod.compatibility.translation.provider.ProviderCalls;
import java.util.Objects;

public final class ProviderHandoff implements AutoCloseable {
    public static final int LAYOUT_COLOR_ATTACHMENT = 0x958E;
    public static final int LAYOUT_DEPTH_STENCIL_ATTACHMENT = 0x958F;
    private static final long[] NO_ARGUMENTS = new long[0];
    private static final int GL_HANDLE_TYPE_OPAQUE_FD = 0x9586;
    private static final long NO_BUFFERS = 0;
    private final ProviderCalls provider;
    private final NativeMemory memory;
    private final int textureCount;
    private final long textures;
    private final long layouts;
    private final int rendererSignal;
    private final int providerSignal;

    public ProviderHandoff(
            ProviderCalls provider,
            NativeMemory memory,
            int[] sharedTextures,
            int[] textureLayouts,
            int rendererSignal,
            int providerSignal) {
        if (sharedTextures.length == 0 || sharedTextures.length != textureLayouts.length) {
            throw new IllegalArgumentException("every shared texture is handed over in one layout");
        }
        this.provider = Objects.requireNonNull(provider, "provider");
        this.memory = Objects.requireNonNull(memory, "memory");
        this.textureCount = sharedTextures.length;
        this.textures = memory.allocate(textureCount * Integer.BYTES);
        this.layouts = memory.allocate(textureCount * Integer.BYTES);
        NativeInts.write(memory, textures, sharedTextures);
        NativeInts.write(memory, layouts, textureLayouts);
        this.rendererSignal = rendererSignal;
        this.providerSignal = providerSignal;
    }

    public static int importSemaphore(ProviderCalls provider, ScratchInts scratch, long fileDescriptor) {
        provider.invoke("glGenSemaphoresEXT", new long[] {1, scratch.address()});
        int semaphore = scratch.first();
        provider.invoke("glImportSemaphoreFdEXT", new long[] {semaphore, GL_HANDLE_TYPE_OPAQUE_FD, fileDescriptor});
        return semaphore;
    }

    public void waitForRenderer() {
        provider.invoke("glWaitSemaphoreEXT", semaphoreCall(rendererSignal));
    }

    public void signalAndFlush() {
        provider.invoke("glSignalSemaphoreEXT", semaphoreCall(providerSignal));
        provider.invoke("glFlush", NO_ARGUMENTS);
    }

    public void finish() {
        provider.invoke("glFinish", NO_ARGUMENTS);
    }

    @Override
    public void close() {
        memory.free(textures);
        memory.free(layouts);
    }

    private long[] semaphoreCall(int semaphore) {
        return new long[] {semaphore, 0, NO_BUFFERS, textureCount, textures, layouts};
    }
}
